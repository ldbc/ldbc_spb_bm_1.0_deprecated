package eu.ldbc.semanticpublishing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.agents.AbstractAsynchronousAgent;
import eu.ldbc.semanticpublishing.agents.AggregationAgent;
import eu.ldbc.semanticpublishing.agents.EditorialAgent;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.enterprise.ReplicationAndBackupHelper;
import eu.ldbc.semanticpublishing.generators.data.DataGenerator;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.CreativeWorksAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.GeonamesAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.ReferenceDataAnalyzer;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParametersManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.LoggingUtil;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.ShellUtil;
import eu.ldbc.semanticpublishing.util.StringUtil;
import eu.ldbc.semanticpublishing.util.ThreadUtil;
import eu.ldbc.semanticpublishing.validation.AggregateOperationsValidator;
import eu.ldbc.semanticpublishing.validation.EditorialOperationsValidator;
import eu.ldbc.semanticpublishing.validation.ValidationValuesManager;

/**
 * The start point of the semantic publishing test driver. Initializes and runs all parts of the benchmark.
 */
public class TestDriver {
	private int aggregationAgentsCount;
	private int editorialAgentsCount;
	private int warmupPeriodSeconds;
	private int benchmarkRunPeriodSeconds;
	private SparqlQueryExecuteManager queryExecuteManager;
	private final AtomicBoolean inBenchmarkState = new AtomicBoolean(false);
	private final AtomicBoolean keepObserverAlive = new AtomicBoolean(false);
	private final AtomicBoolean benchmarkResultIsValid = new AtomicBoolean(false);
	
	private final Configuration configuration = new Configuration();
	private final Definitions definitions = new Definitions();
	private final MustacheTemplatesHolder mustacheTemplatesHolder = new MustacheTemplatesHolder();
	private final RandomUtil randomGenerator;
	private final SubstitutionQueryParametersManager substitutionQueryParamtersManager = new SubstitutionQueryParametersManager();
	private final ValidationValuesManager validationValuesManager = new ValidationValuesManager();
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	private final static Logger RLOGGER = LoggerFactory.getLogger(BenchmarkProcessObserver.class.getName());
	
	public TestDriver(String[] args) throws IOException {
		
		if( args.length < 1) {
			throw new IllegalArgumentException("Missing parameter - the configuration file must be specified");
		}
		configuration.loadFromFile(args[0]);
		definitions.loadFromFile(configuration.getString(Configuration.DEFINITIONS_PATH), configuration.getBoolean(Configuration.VERBOSE));
		mustacheTemplatesHolder.loadFrom(configuration.getString(Configuration.QUERIES_PATH));

		//will read the dictionary file from jar file as a resource
		randomGenerator = initializeRandomUtil(configuration.getString(Configuration.REFERENCE_DATASETS_PATH), configuration.getLong(Configuration.GENERATOR_RANDOM_SEED), definitions.getInt(Definitions.YEAR_SEED), definitions.getInt(Definitions.DATA_GENERATOR_PERIOD_YEARS));
		
		//will use initialized randomGenerator above
		definitions.initializeAllocations(randomGenerator.getRandom());
		
		aggregationAgentsCount = configuration.getInt(Configuration.AGGREGATION_AGENTS_COUNT);
		editorialAgentsCount = configuration.getInt(Configuration.EDITORIAL_AGENTS_COUNT);
		warmupPeriodSeconds = configuration.getInt(Configuration.WARMUP_PERIOD_SECONDS);
		benchmarkRunPeriodSeconds = configuration.getInt(Configuration.BENCHMARK_RUN_PERIOD_SECONDS);


		queryExecuteManager = new SparqlQueryExecuteManager(inBenchmarkState,
				configuration.getString(Configuration.ENDPOINT_URL),
				configuration.getString(Configuration.ENDPOINT_UPDATE_URL),
				configuration.getInt(Configuration.QUERY_TIMEOUT_SECONDS) * 1000,
				configuration.getInt(Configuration.SYSTEM_QUERY_TIMEOUT_SECONDS) * 1000,
				configuration.getBoolean(Configuration.VERBOSE));
		
		//set the nextId for Creative Works, default 0
		DataManager.creativeWorksNextId.set(configuration.getLong(Configuration.CREATIVE_WORK_NEXT_ID));
	}
	
	public SparqlQueryExecuteManager getQueryExecuteManager() { 
		return queryExecuteManager;
	}
	
	private RandomUtil initializeRandomUtil(String datasetsPath, long seed, int yearSeed, int generorPeriodYears) {
		//File WordsDictionary.txt is one level up
		String ontPath = StringUtil.normalizePath(datasetsPath);
		String oneLevelUp = ontPath.substring(0, ontPath.lastIndexOf(File.separator) + 1);		
		String filePath = oneLevelUp + "dictionaries" + File.separator + "WordsDictionary.txt";		
		
		return new RandomUtil(filePath, seed, yearSeed, generorPeriodYears);
	}

	private void loadOntologies(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading ontologies...");
			
			String ontologiesPath = StringUtil.normalizePath(configuration.getString(Configuration.ONTOLOGIES_PATH));
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
			
			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(ontologiesPath, collectedFiles, "ttl", true);
			
			Collections.sort(collectedFiles);
			
			for( File file : collectedFiles ) {
				System.out.print("\tloading " + file.getName());
				InputStream input = new FileInputStream(file);
				
				RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_TURTLE, input);
				System.out.println();
			}			
		}
	}
	
	private void adjustRefDatasetsSizes(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Adjusting reference datasets size...");
			int magnitudeOfEntities = 100000;//magnitude in terms of entities used to get from reference dataset
			int avgTriplesPerCw = 19;//average number of triples per Creative Work
			String dpbediaPrefix = "dbpedia";
			String personEntityTypeUri = "http://xmlns.com/foaf/0.1/Person"; // foaf:Person
			String datasetsPath = StringUtil.normalizePath(configuration.getString(Configuration.REFERENCE_DATASETS_PATH));
			
			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(datasetsPath, collectedFiles, "adjustablettl", true);
			
			//calculate the amount of triples to be used for reference knowledge. Value is related to current size of the dataset to be generated.
			//triplesLimit = Log10(CreativeWorksCount) * magnitudeTriples
			long entitiesLimit = (long) (Math.log10(configuration.getLong(Configuration.DATASET_SIZE_TRIPLES) / avgTriplesPerCw) * magnitudeOfEntities);
			
			for( File file : collectedFiles ) {
				if (file.getPath().contains(dpbediaPrefix)) {
					System.out.println("\tAdjusting entities size for file : " + file.getName() + ", entities to be used : " + entitiesLimit);
					RdfUtils.cropDatasetFile(file.getPath(), datasetsPath + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")) + ".adjusted.ttl", entitiesLimit, personEntityTypeUri);		
				}
			}
		}
	}
	 
	private void loadDatasets(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading reference datasets...");
			
			String datasetsPath = StringUtil.normalizePath(configuration.getString(Configuration.REFERENCE_DATASETS_PATH));
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
			
			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(datasetsPath, collectedFiles, "ttl", true);
			
			Collections.sort(collectedFiles);
			
			for( File file : collectedFiles ) {
				System.out.print("\tloading " + file.getName());
				InputStream input = new FileInputStream(file);
				
				RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_TURTLE, input);
				System.out.println();
			}				
		}
	}
	
	public void populateRefDataEntitiesLists(boolean showDetails, boolean populateFromDatasetInfoFile, boolean suppressDatasetInfoWarnings, String messagePrefix) throws IOException {
		
		if (showDetails) {
			System.out.println(messagePrefix + "Analyzing reference knowledge in data, it may take a while...");
		}
		
		//retrieve entity URIs from database
		ReferenceDataAnalyzer refDataAnalyzer = new ReferenceDataAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		ArrayList<Entity> entitiesList = refDataAnalyzer.analyzeEntities();
		for (Entity e : entitiesList) {
			//popular ?
			if (Definitions.entityPopularity.getAllocation() == 0) {
				DataManager.popularEntitiesList.add(e);
			} else {
				DataManager.regularEntitiesList.add(e);
			}
		}

		//retrieve the greatest id of creative works from database
		CreativeWorksAnalyzer cwk = new CreativeWorksAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		long count = cwk.getResult();		
		DataManager.creativeWorksNextId.set(count);
		
		//retrieve geonames IDs from database
		GeonamesAnalyzer gna = new GeonamesAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		ArrayList<String> geonamesIds = gna.collectGeonamesIds();
		for (String s : geonamesIds) {
			DataManager.geonamesIdsList.add(s);
		}
		
		//initialize dataset info, required for query parameters
		if (populateFromDatasetInfoFile) {
			if ((DataManager.correlatedEntitiesList.size() + DataManager.exponentialDecayEntitiesMinorList.size() + DataManager.exponentialDecayEntitiesMajorList.size()) == 0) {
				String datasetInfoFile = DataManager.buildDataInfoFilePath(configuration);
				if (!datasetInfoFile.isEmpty()) {			
					DataManager.initDatasetInfo(datasetInfoFile, suppressDatasetInfoWarnings);
				}
			}
		}
		
		if (configuration.getBoolean(Configuration.VERBOSE) && showDetails) {
			System.out.println(messagePrefix + "\t(reference data entities size : " + entitiesList.size() + ", max Creative Work id : " + count + ", geonames entities size : " + geonamesIds.size() + ")");
		}
	}
	
	public void loadCreativeWorks(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading Creative Works...");
			
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
			
			File[] files = new File(configuration.getString(Configuration.CREATIVE_WORKS_PATH)).listFiles();
			
			Arrays.sort(files);
			int size=0;
			long startTime = System.currentTimeMillis();
			for( File file : files ) {
				size++;
				if( file.getName().endsWith(".nq")) {
					System.out.print("\tloading " + file.getName());
					InputStream input = new FileInputStream(file);
					
					RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_SESAME_NQUADS, input);
					System.out.println();
				}
				if( file.getName().endsWith(".ttl")) {
					System.out.print("\tloading " + file.getName());
					InputStream input = new FileInputStream(file);
					
					RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_TURTLE, input);
					System.out.println();
				}		
			}
			long endTime = System.currentTimeMillis();
			System.out.println("Loaded "+size+" files with Creative Works in "+ (endTime - startTime) + " milliseconds");
		}
	}
	
	private void generateCreativeWorks(boolean enable) throws IOException, InterruptedException {
		if (enable) {
			System.out.println("Generating Creative Works data files...");
			
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, false, true, "");
			}
			
			long triplesPerFile = configuration.getLong(Configuration.GENERATED_TRIPLES_PER_FILE);
			long totalTriples = configuration.getLong(Configuration.DATASET_SIZE_TRIPLES);
			String destinationPath = configuration.getString(Configuration.CREATIVE_WORKS_PATH);
			String serializationFormat = configuration.getString(Configuration.GENERATE_CREATIVE_WORKS_FORMAT);
			
			int generatorThreads = configuration.getInt(Configuration.DATA_GENERATOR_WORKERS);
			
			DataGenerator dataGenerator = new DataGenerator(randomGenerator, configuration, definitions, generatorThreads, totalTriples, triplesPerFile, destinationPath, serializationFormat);
			dataGenerator.produceData();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void generateQuerySubstitutionParameters(boolean enable) throws InterruptedException, IOException {
		if (enable) {
			System.out.println("Generating query parameters");
			
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
			}
			
			if (DataManager.creativeWorksNextId.get() == 0) {
				System.out.println("\tNo creative works were found in the database, load creative works first! aborting...");
				return;
			}

			String targetFolder = configuration.getString(Configuration.CREATIVE_WORKS_PATH);
			FileUtils.makeDirectories(targetFolder);
			
			BufferedWriter bw = null;
			Class<SubstitutionParametersGenerator> c = null;
			Constructor<?> cc = null;
			SubstitutionParametersGenerator queryTemplate = null;
			try {
/*
				//Editorial query parameters
				//Insert
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "insert"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "InsertTemplate"));
				cc = c.getConstructor(String.class, RandomUtil.class, HashMap.class, int.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance("", randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions.getInt(Definitions.YEAR_SEED));			
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();

				//Update
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "update"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "UpdateTemplate"));
				cc = c.getConstructor(String.class, RandomUtil.class, HashMap.class, int.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance("", randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions.getInt(Definitions.YEAR_SEED));
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();

				//Delete
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "delete"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "DeleteTemplate"));
				cc = c.getConstructor(RandomUtil.class, HashMap.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance(randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL));
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();
*/
				
				//Aggregate query parameters
				for (int i = 1; i <= Statistics.AGGREGATE_QUERIES_COUNT; i++) {
					bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("query%01dSubstParameters", i) + ".txt")));
					
					c = (Class<SubstitutionParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.aggregation.Query%dTemplate", i));
					cc = c.getConstructor(RandomUtil.class, HashMap.class, Definitions.class, String[].class);
					queryTemplate = (SubstitutionParametersGenerator) cc.newInstance(randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), definitions, null);					
					queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
					
					bw.close();
					
					//indicate activity in console
					if (i != Statistics.AGGREGATE_QUERIES_COUNT) {
						System.out.print(".");
					} else {
						System.out.println(".");
					}
				}
				System.out.println("\n");
			} catch (Exception e) {
				System.out.println("\n\tException caught during generation of query substitution parameters : " + e.getClass().getName() + " :: " + e.getMessage());
			} finally {
				try {bw.close();} catch(Exception e) {};
			}
		}
	}	
	
	public void initializeQuerySubstitutionParameters(boolean enable) throws IOException, InterruptedException {
		if (enable) {
			boolean validationPhaseIsEnabled = configuration.getBoolean(Configuration.VALIDATE_QUERY_RESULTS);
			
			if (!validationPhaseIsEnabled) {
				System.out.println("Initializing query substitution parameters...");
			}
			substitutionQueryParamtersManager.intiSubstitutionParameters(configuration.getString(Configuration.CREATIVE_WORKS_PATH), validationPhaseIsEnabled, true);
		}
	}
	
	public void validateQueryResults(boolean enable) throws Exception {
		if (enable) {
			System.out.println("Validating operations...");
			
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(false, true, true, "\t");
			}
			validationValuesManager.intiValidationValues(configuration.getString(Configuration.VALIDATION_PATH), false);

			EditorialOperationsValidator eov = new EditorialOperationsValidator(queryExecuteManager, randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.VALIDATION), configuration, definitions);
			eov.validate();
			
			//refresh info about reference data and CWs stored in database 
			populateRefDataEntitiesLists(false, true, true, "");
			
			AggregateOperationsValidator aov = new AggregateOperationsValidator(this, validationValuesManager, queryExecuteManager, randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), configuration, definitions);
			aov.validate();
		}
	}

	private final AtomicBoolean runFlag = new AtomicBoolean(true);
	
	private void setupAsynchronousAgents() {
		for(int i = 0; i < aggregationAgentsCount; ++i ) {
			aggregationAgents.add(new AggregationAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), definitions, substitutionQueryParamtersManager));
		}

		for(int i = 0; i < editorialAgentsCount; ++i ) {
			editorialAgents.add(new EditorialAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions));
		}
	}
	
	private final List<AbstractAsynchronousAgent> aggregationAgents = new ArrayList<AbstractAsynchronousAgent>();
	private final List<AbstractAsynchronousAgent> editorialAgents = new ArrayList<AbstractAsynchronousAgent>();
	private boolean aggregationAgentsStarted = false;
	private boolean editorialAgentsStarted = false;
	
	private void warmUp(boolean enable) throws IOException {
		if (enable) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}
			
			String message = "Warming up...";
			
			System.out.println(message);
			LOGGER.info(message);
			
			aggregationAgentsStarted = true;

			for(int i = 0; i < aggregationAgentsCount; ++i ) {
				aggregationAgents.get(i).start();
			}

			ThreadUtil.sleepSeconds(warmupPeriodSeconds);
		}
	}
	
	private void benchmark(boolean enable, long benchmarkByQueryRuns, double mileStonePosition) throws IOException {
		if (enable) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}
			
			if (configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP)) {
				System.out.println("Warning : runBenchmark and runBenchmarkWithOnlineReplication phases are both enabled, disable one first!");
				System.exit(-1);
			}			
			
			if (benchmarkByQueryRuns > 0 && aggregationAgentsCount <= 0) {
				System.out.println(String.format("Warning : aggregation agents amount : %d is not acceptable for execution of the benchmark in that mode, exiting...", aggregationAgentsCount));
				System.exit(-1);
			}
			
			String message;
			
			if (benchmarkByQueryRuns > 0) {
				message = String.format("Starting the benchmark... (will run until %d aggregate executions have been completed)", benchmarkByQueryRuns);
			} else {				
				message = "Starting the benchmark...";
			}
			
			System.out.println(message);
			LOGGER.info(message);

			inBenchmarkState.set(true);
			
			if(!aggregationAgentsStarted) {
				aggregationAgentsStarted = true;
				for(AbstractAsynchronousAgent agent : aggregationAgents ) {
					if( ! agent.isAlive()) {
						agent.start();
					}
				}
			}
			
			editorialAgentsStarted = true;
			for(AbstractAsynchronousAgent agent : editorialAgents ) {
				agent.start();
			}

			Thread observerThread = new BenchmarkProcessObserver(Statistics.totalAggregateQueryStatistics.getRunsCountAtomicLong(), 
													   		     inBenchmarkState, 
													   		     keepObserverAlive,
													   		     benchmarkResultIsValid,
													   		     configuration.getDouble(Configuration.UPDATE_RATE_THRESHOLD_REACH_TIME_PERCENT),
													   		     configuration.getDouble(Configuration.UPDATE_RATE_THRESHOLD_OPS),
																 configuration.getInt(Configuration.EDITORIAL_AGENTS_COUNT),																				
																 configuration.getInt(Configuration.AGGREGATION_AGENTS_COUNT), 
																 configuration.getLong(Configuration.BENCHMARK_RUN_PERIOD_SECONDS),
																 benchmarkByQueryRuns, 
																 configuration.getBoolean(Configuration.VERBOSE));
			observerThread.start();
			
			if (benchmarkByQueryRuns > 0) {
				while (Statistics.totalAggregateQueryStatistics.getRunsCount() < benchmarkByQueryRuns) {
					ThreadUtil.sleepMilliseconds(50);					
				}
			} else {
				ThreadUtil.sleepSeconds(benchmarkRunPeriodSeconds);
			}
			
			inBenchmarkState.set(false);
			
			ThreadUtil.join(observerThread);
			
			if (!benchmarkResultIsValid.get()) {
				message = String.format("Warning : Benchmark results are not valid! Required query rate has not been reached, or has dropped below threshold (%.1f ops) during the benchmark run.", configuration.getDouble(Configuration.UPDATE_RATE_THRESHOLD_OPS));
			} else {
				message = "Benchmark result is valid!";
			}
			System.out.println(message);
			LOGGER.info(message);								
			
			message = "Stopping the benchmark...";
			System.out.println(message);
			LOGGER.info(message);			
		}
	}
	
	/**
	 * @param enable 				 - enable the phase
	 * @param benchmarkByQueryRuns   - if set to zero, then time interval set by parameter 'benchmarkRunPeriodSeconds' will be used for completing the phase.
	 * 								   if greater than zero, then its value the amount of aggregate queries that will be executed for completing the phase.
	 * @param mileStonePosition - defines after the position of execution of a 'mileStone' query ( the query that will verify that certain milestone has been reached). 
	 * 								   This parameter is considered only if benchmarkByQueryRuns > 0. 
	 * 								   e.g. if mileStoneQueryPosition = 0.2 in terms of percents, then after 20% of executed queries a mileStone query is started.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private void benchmarkOnlineReplicationAndBackup(boolean enable, long benchmarkByQueryRuns, double milestonePosition) throws IOException, InterruptedException {
		if (enable) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}

			if (configuration.getBoolean(Configuration.RUN_BENCHMARK)) {
				System.out.println("Warning : runBenchmark and runBenchmarkWithOnlineReplication phases are both enabled, disable one first!");
				System.exit(-1);
			}			
			
			if (benchmarkByQueryRuns > 0 && aggregationAgentsCount <= 0) {
				System.out.println(String.format("Warning : aggregation agents amount : %d is not acceptable for execution of the benchmark in that mode, exiting...", aggregationAgentsCount));
				System.exit(-1);
			}
			
			String message;
			
			if (benchmarkByQueryRuns > 0) {
				message = String.format("Starting the benchmark... (will run until %d aggregate executions have been completed)", benchmarkByQueryRuns);
				System.out.println(message);
				LOGGER.info(message);				
			} else {				
				message = "Warning : The benchmark driver is not configured properly, set a positive value to property 'benchmarkByQueryRuns'. Exiting.";
				System.out.println(message);
				LOGGER.info(message);								
				System.exit(-1);				
			}
			
			inBenchmarkState.set(true);
			keepObserverAlive.set(true);
			
			if(!aggregationAgentsStarted) {
				aggregationAgentsStarted = true;
				for(AbstractAsynchronousAgent agent : aggregationAgents ) {
					if( ! agent.isAlive()) {
						agent.start();
					}
				}
			}
			
			editorialAgentsStarted = true;
			for(AbstractAsynchronousAgent agent : editorialAgents ) {
				agent.start();
			}
			
			Thread observerThread = new BenchmarkProcessObserver(Statistics.totalAggregateQueryStatistics.getRunsCountAtomicLong(), 
													   		     inBenchmarkState,
													   		     keepObserverAlive, 
													   		     benchmarkResultIsValid,
													   		     configuration.getDouble(Configuration.UPDATE_RATE_THRESHOLD_REACH_TIME_PERCENT),
													   		     0.0,									   		     
																 configuration.getInt(Configuration.EDITORIAL_AGENTS_COUNT),																				
																 configuration.getInt(Configuration.AGGREGATION_AGENTS_COUNT), 
																 configuration.getLong(Configuration.BENCHMARK_RUN_PERIOD_SECONDS),
																 benchmarkByQueryRuns, 
																 configuration.getBoolean(Configuration.VERBOSE));
			observerThread.start();
			
			String[] milestoneSubstitutionParameters = null;
			ReplicationAndBackupHelper replicationHelper = new ReplicationAndBackupHelper(queryExecuteManager, randomGenerator, configuration, definitions, mustacheTemplatesHolder);
			boolean milestoneQueryExecuted = false;
			
			try {
				while (Statistics.totalAggregateQueryStatistics.getRunsCount() < benchmarkByQueryRuns) {
					ThreadUtil.sleepMilliseconds(50);
					
					if (!milestoneQueryExecuted && (Statistics.totalAggregateQueryStatistics.getRunsCount() >= benchmarkByQueryRuns * milestonePosition)) {					
						//Milestone point reached, mark it by executing a milestone INSERT query
						message = "Setting a milestone before starting incremental backup...";
						System.out.println(message);
						LOGGER.info(message);							
						milestoneSubstitutionParameters = replicationHelper.executeMilestoneQuery(1);
						milestoneQueryExecuted = true;
						
						//Start incremental backup
						message = "Starting incremental backup (incremental_backup_start)...";
						System.out.println(message);
						LOGGER.info(message);							
						ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.INCREMENTAL_BACKUP_START + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
					}
				}
			} catch (IOException ioe) {
				inBenchmarkState.set(false);
				message = "Warning : Stopping the benchmark : IOExcetion : " + ioe.getMessage();
				System.out.println(message);
				throw new IOException(ioe);
			}
			
			//stop all agents, but keep measuring until database has been restarted and milestone point has been confirmed
			inBenchmarkState.set(false);
						
			message = "Shutting down the database (system_shutdown)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.SYSTEM_SHUTDOWN + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
			
			//update query timeout value to allow longer timeouts for milestone validation queries. In cases when startup of the database requires extra time to recover.
			replicationHelper.updateQueryExecutionTimeout(48*60*60*1000);
			
			message = "Starting up the database (system_startup)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.SYSTEM_START + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);

			message = "Verifying that milestone point exists";
			System.out.println(message);
			LOGGER.info(message);
			
			if (replicationHelper.validateMilestoneQuery(milestoneSubstitutionParameters)) {
				message = "\tOK : milestone point found!";				
			} else {
				message = "\tError : milestone doesn't exist";
			}
			System.out.println(message);
			LOGGER.info(message);

			keepObserverAlive.set(false);
			
			message = "Stopping the benchmark...";
			System.out.println(message);
			LOGGER.info(message);
			
			ThreadUtil.join(observerThread);
			
			if (!benchmarkResultIsValid.get()) {
				message = String.format("Warning : Benchmark results are not valid! Required query rate has not been reached, or has dropped below threshold (%.1f ops) during the benchmark run.", configuration.getDouble(Configuration.UPDATE_RATE_THRESHOLD_OPS));
			} else {
				message = "Benchmark result is valid!";
			}
			System.out.println(message);
			LOGGER.info(message);	
			
			message = "Verifying milestone points...\nShutting down the database (system_shutdown)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.SYSTEM_SHUTDOWN + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
						
			message = "Starting up the database (system_startup)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.SYSTEM_START + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
			
			message = "Restoring state from full backup (before the warmup and benchmarking phases) (full_backup_restore)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.FULL_BACKUP_RESTORE + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);

			message = "Starting up the database (system_startup)...";
			System.out.println(message);
			LOGGER.info(message);
			ShellUtil.execute(StringUtil.normalizePath(configuration.getString(Configuration.ENTERPRISE_FEATURES_PATH)) + File.separator + "scripts", ReplicationAndBackupHelper.SYSTEM_START + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
			
			message = "Verifying that current state of the database doesn't contain the milestone point";
			System.out.println(message);
			LOGGER.info(message);
			
			if (replicationHelper.validateMilestoneQuery(milestoneSubstitutionParameters)) {
				message = "\tError : milestone point found! It shouldn't exist after restoring from full backup taken before the benchmark run.";				
			} else {
				message = "\tOK : milestone point doesn't exist!";
			}
			System.out.println(message);
			LOGGER.info(message);				
		}
	}
	
	private void stopAynchronousAgents() {
		runFlag.set(false);
		
		if( aggregationAgentsStarted ) {
			for(AbstractAsynchronousAgent agent : aggregationAgents ) {
				ThreadUtil.join(agent);
			}
		}
		
		if( editorialAgentsStarted ) {
			for(AbstractAsynchronousAgent agent : editorialAgents ) {
				ThreadUtil.join(agent);
			}
		}		
	}
	
	private void checkConformance(boolean enable) throws IOException {
		if (enable) {

			String queriesPath = configuration.getString(Configuration.QUERIES_PATH) + File.separator + "conformance";	
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
			
			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(queriesPath, collectedFiles, "ttl", true);
			
			Collections.sort(collectedFiles);
			
			System.out.println("Preparing for conformance tests...");
			for( File file : collectedFiles ) {
				System.out.print("\tloading " + file.getName());
				InputStream input = new FileInputStream(file);
				
				RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_TURTLE, input);
				System.out.println();
			}						
			
			collectedFiles.clear();
			
			//Collect Conformance Queries
			List<String> collectedQueries = new ArrayList<String>();
			FileUtils.collectFilesList(queriesPath, collectedQueries, "txt", true);
			
			Collections.sort(collectedQueries);
			
			StringBuilder resultSb = new StringBuilder();
			resultSb.append("\n\nTesting conformance capabilities...\n\n");
			for (String filePath : collectedQueries) {
				boolean askQuery = false;
				if (filePath.contains("-ask")) {
					askQuery = true;
				}
				boolean skipReporting = false;
				if (filePath.contains("-skipreporting")) {
					skipReporting = true;
				}
				
				StringBuilder sb = new StringBuilder();
				String[] queryArray = FileUtils.readTextFile(filePath);
				String constraintTest = queryArray[0].startsWith("#") ? queryArray[0] : "";
	
				for (int i = 1; i < queryArray.length; i++) {
					if (queryArray[i].trim().startsWith("#")) {
						continue;
					}
					sb.append(queryArray[i]);
				}
				
				boolean constraintViolationCheckSucceeded = false;
				try {
					if (askQuery) {
						String queryResult = queryExecuteManager.executeQuery(constraintTest, sb.toString(), QueryType.SELECT);
						if (queryResult.toLowerCase().contains("<boolean>true</boolean>")) {
							constraintViolationCheckSucceeded = true;
						}
					} else {
						queryExecuteManager.executeQuery(constraintTest, sb.toString(), QueryType.INSERT);
					}
				} catch (IOException ioe) {
					//deliberately catching IOException from queryExecuteManager as a sign of a successful constraint violation test
					constraintViolationCheckSucceeded = true;
				}
				
				if (!skipReporting) {
					resultSb.append(String.format("\t%-84s : %s\n", constraintTest, constraintViolationCheckSucceeded ? "success" : "failed"));
				}
			}
			System.out.println(resultSb.toString());
			RLOGGER.info(resultSb.toString());
		}
	}
	
	public void clearDatabase(boolean enable) throws IOException {
		if (enable) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, false, true, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warmup : Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}			
			System.out.println("Cleaning up database ...");
			queryExecuteManager.executeQuery("SERVICE-DELETE", " CLEAR ALL ", QueryType.DELETE);
		}
	}

	public void executePhases() throws Exception {
		loadOntologies(configuration.getBoolean(Configuration.LOAD_ONTOLOGIES));
		adjustRefDatasetsSizes(configuration.getBoolean(Configuration.ADJUST_REF_DATASETS_SIZES));
		loadDatasets(configuration.getBoolean(Configuration.LOAD_REFERENCE_DATASETS));
		generateCreativeWorks(configuration.getBoolean(Configuration.GENERATE_CREATIVE_WORKS));
		loadCreativeWorks(configuration.getBoolean(Configuration.LOAD_CREATIVE_WORKS));
		generateQuerySubstitutionParameters(configuration.getBoolean(Configuration.GENERATE_QUERY_SUBSTITUTION_PARAMETERS));
		initializeQuerySubstitutionParameters(configuration.getBoolean(Configuration.WARM_UP) || configuration.getBoolean(Configuration.RUN_BENCHMARK) || configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP));
		validateQueryResults(configuration.getBoolean(Configuration.VALIDATE_QUERY_RESULTS));
		setupAsynchronousAgents();
		warmUp(configuration.getBoolean(Configuration.WARM_UP));
		benchmark(configuration.getBoolean(Configuration.RUN_BENCHMARK), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_RUNS), definitions.getDouble(Definitions.MILESTONE_QUERY_POSITION));
		benchmarkOnlineReplicationAndBackup(configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_RUNS), definitions.getDouble(Definitions.MILESTONE_QUERY_POSITION));
		stopAynchronousAgents();
		checkConformance(configuration.getBoolean(Configuration.CHECK_CONFORMANCE));
		clearDatabase(configuration.getBoolean(Configuration.CLEAR_DATABASE));
		
		System.out.println("END OF BENCHMARK RUN, all agents shut down...");
		System.exit(0);
	}
	
	public static void main(String[] args) throws Exception {
		LoggingUtil.Configure();
		TestDriver testDriver = new TestDriver(args);
		testDriver.executePhases();
	}
}