package eu.ldbc.semanticpublishing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.agents.AbstractAsynchronousAgent;
import eu.ldbc.semanticpublishing.agents.AggregationAgent;
import eu.ldbc.semanticpublishing.agents.EditorialAgent;
import eu.ldbc.semanticpublishing.datagenerator.DataGenerator;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.CreativeWorksAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.GeonamesAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.ReferenceDataAnalyzer;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.LoggingUtil;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.StringUtil;
import eu.ldbc.semanticpublishing.util.ThreadUtil;

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
	
	private final Configuration configuration = new Configuration();
	private final Definitions definitions = new Definitions();
	private final MustacheTemplatesHolder mustacheTemplatesHolder = new MustacheTemplatesHolder();
	private final RandomUtil randomGenerator;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	private final static Logger RLOGGER = LoggerFactory.getLogger(Reporter.class.getName());
	
	public TestDriver(String[] args) throws IOException {
		
		if( args.length < 1) {
			throw new IllegalArgumentException("Missing parameter - the configuration file must be specified");
		}
		configuration.loadFromFile(args[0]);
		definitions.loadFromFile(configuration.getString(Configuration.DEFINITIONS_PATH), configuration.getBoolean(Configuration.VERBOSE));
		mustacheTemplatesHolder.loadFrom(configuration.getString(Configuration.QUERIES_PATH));

		//will read the dictionary file from jar file as a resource
		randomGenerator = initializeRandomUtil();
		
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
	}
	
	public SparqlQueryExecuteManager getQueryExecuteManager() { 
		return queryExecuteManager;
	}
	
	private RandomUtil initializeRandomUtil() {
		//File WordsDictionary.txt is one level up
		String ontologiesPath = StringUtil.normalizePath(configuration.getString(Configuration.ONTOLOGIES_PATH));
		String oneLevelUp = ontologiesPath.substring(0, ontologiesPath.lastIndexOf(File.separator) + 1);
		String filePath = oneLevelUp + "WordsDictionary.txt";
		
		return new RandomUtil(filePath, configuration.getLong(Configuration.GENERATOR_RANDOM_SEED), definitions.getInt(Definitions.YEAR_SEED));
	}
	
	private void loadOntologies() throws IOException {
		if( configuration.getBoolean(Configuration.LOAD_ONTOLOGIES)) {
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
	
	private void adjustRefDatasetsSizes() throws IOException {
		if ( configuration.getBoolean(Configuration.ADJUST_REF_DATASETS_SIZES)) {
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
	 
	private void loadDatasets() throws IOException {
		if( configuration.getBoolean(Configuration.LOAD_REFERENCE_DATASETS)) {
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
	
	private void populateRefDataEntitiesLists() throws IOException {
		
		if (configuration.getBoolean(Configuration.VERBOSE)) {
			System.out.println("Retrieving stats from reference knowledge...");
		}
		
		//retrieve entity uris frsom database
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
		DataManager.creativeWorksNexId.set(count);
		
		//retrieve geonames ids from database
		GeonamesAnalyzer gna = new GeonamesAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		ArrayList<String> geonamesIds = gna.collectGeonamesIds();
		for (String s : geonamesIds) {
			DataManager.geonamesIdsList.add(s);
		}
		
		if (configuration.getBoolean(Configuration.VERBOSE)) {
			System.out.println("Ref. Dataset Entities count : " + entitiesList.size() + ", Max CW id : " + count + ", Geonames entities count : " + geonamesIds.size());
		}
	}
	
	private void loadCreativeWorks() throws IOException {
		if( configuration.getBoolean(Configuration.LOAD_CREATIVE_WORKS)) {
			System.out.println("Loading Creative Works...");
			
			String path = configuration.getString(Configuration.CREATIVE_WORKS_PATH);
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
			
			File[] files = new File(path).listFiles();
			
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
	
	private void generateCreativeWorks() throws IOException, InterruptedException {
		if( configuration.getBoolean(Configuration.GENERATE_CREATIVE_WORKS)) {
			System.out.println("Generating Creative Works data files...");
			
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists();
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

/*
 * Method is commented out, using DataGenerator class instead
*/
/*	
	private void generateCreativeWorksSesame(String serializationFormat, long maxTriplesInFile, long targetTriplesSize, String destinationPath) throws IOException {
		
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);
		
		FileUtils.makeDirectories(destinationPath);

		int filesCount = 0;
		long creativeWorksInDatabase = DataManager.creativeWorksNexId.get();
		
		if (creativeWorksInDatabase > 0) {
			System.out.println("\t" + creativeWorksInDatabase + " Creative Works currently exist.");
		}

		//loop until the maximum number of triples to store in the database is reached
		for (long totalTriplesCount = 0; totalTriplesCount < targetTriplesSize; ) {

			//Adjust the number of triples in last batch to be sent
			long triplesLeftInFile = maxTriplesInFile;
			if ((targetTriplesSize - totalTriplesCount) >= maxTriplesInFile) {
				triplesLeftInFile = maxTriplesInFile;
			} else {
				triplesLeftInFile = targetTriplesSize - totalTriplesCount;
			}
			
			String fileName = String.format("%s%sgeneratedCreativeWorks-%04d." + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, ++filesCount);
			
			FileOutputStream fos = null;			
			
			try {
				fos = new FileOutputStream(fileName);
				RDFWriter rdfWriter = Rio.createWriter(rdfFormat, fos);
				
				rdfWriter.startRDF();
	
				int cwsInFileCount = 0;
				int currentQueryTriplesCount = 0;
				while ( currentQueryTriplesCount < triplesLeftInFile ) {
					InsertTemplate insertQuery = new InsertTemplate("", randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL)); 
					Model sesameModel = insertQuery.buildSesameModel();
	
					for (Statement statement : sesameModel) {
						rdfWriter.handleStatement(statement);
					}
					
					cwsInFileCount++;
					currentQueryTriplesCount += sesameModel.size();
				}
				totalTriplesCount += currentQueryTriplesCount;
				
				rdfWriter.endRDF();

				System.out.println("Saving file #" + filesCount + " with " + cwsInFileCount + " Creative Works, total triples : " + totalTriplesCount + ". Targeted triples size in repository : " + targetTriplesSize);
			} catch (RDFHandlerException e) {
				throw new IOException("A problem occurred generating RDF data: " + e.getMessage());
			}
			finally {
				if(fos != null) {
					fos.close();
				}
			}
		}
		System.out.println("\tcompleted! Total Creative Works saved : " + (DataManager.creativeWorksNexId.get() - creativeWorksInDatabase) + " in " + filesCount + " files.");
	}
*/
	private final AtomicBoolean runFlag = new AtomicBoolean(true);
	
	private void setupAsynchronousAgents() {
		for(int i = 0; i < aggregationAgentsCount; ++i ) {
			aggregationAgents.add(new AggregationAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION)));
		}

		for(int i = 0; i < editorialAgentsCount; ++i ) {
			editorialAgents.add(new EditorialAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions.getInt(Definitions.YEAR_SEED)));
		}
	}
	
	private final List<AbstractAsynchronousAgent> aggregationAgents = new ArrayList<AbstractAsynchronousAgent>();
	private final List<AbstractAsynchronousAgent> editorialAgents = new ArrayList<AbstractAsynchronousAgent>();
	private boolean aggregationAgentsStarted = false;
	private boolean editorialAgentsStarted = false;
	
	private void warmUp() throws IOException {
		if( configuration.getBoolean(Configuration.WARM_UP)) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists();
				if (DataManager.creativeWorksNexId.get() == 0) {
					System.err.println("Warmup : Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}			
			
			System.out.println("Warming up...");
			LOGGER.info("Warming up...");
			
			aggregationAgentsStarted = true;

			for(int i = 0; i < aggregationAgentsCount; ++i ) {
				aggregationAgents.get(i).start();
			}

			ThreadUtil.sleepSeconds(warmupPeriodSeconds);
		}
	}
	
	private void benchmark() throws IOException {
		if( configuration.getBoolean(Configuration.RUN_BENCHMARK)) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists();
				if (DataManager.creativeWorksNexId.get() == 0) {
					System.err.println("Warmup : Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}
			
			System.out.println("Starting the benchmark...");
			LOGGER.info("Starting the benchmark...");

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

			reporterAgentScheduledService.scheduleAtFixedRate(new Reporter(inBenchmarkState, 
																				configuration.getInt(Configuration.EDITORIAL_AGENTS_COUNT),																				
																				configuration.getInt(Configuration.AGGREGATION_AGENTS_COUNT), 
																				configuration.getLong(Configuration.BENCHMARK_RUN_PERIOD_SECONDS),
																				configuration.getBoolean(Configuration.VERBOSE) ), 
															  1000, 
															  1000, 
															  TimeUnit.MILLISECONDS);
			
			ThreadUtil.sleepSeconds(benchmarkRunPeriodSeconds);
			
			System.out.println("Stopping the benchmark...");
			LOGGER.info("Stopping the benchmark...");			
			
			inBenchmarkState.set(false);
			reporterAgentScheduledService.shutdownNow();
		}
	}
	
	private ScheduledThreadPoolExecutor reporterAgentScheduledService = new ScheduledThreadPoolExecutor(1);

	
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
	
	private void checkConformance() throws IOException {
		if( configuration.getBoolean(Configuration.CHECK_CONFORMANCE)) {

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
	
	private void clearDatabase() throws IOException {
		if( configuration.getBoolean(Configuration.CLEAR_DATABASE)) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists();
				if (DataManager.creativeWorksNexId.get() == 0) {
					System.err.println("Warmup : Warning : no Creative Works were found stored in the database, initialise it with ontologies and reference datasets first! Exiting.");
					System.exit(-1);
				}
			}			
			System.out.println("Cleaning up database ...");
			queryExecuteManager.executeQuery("SERVICE-DELETE", " CLEAR ALL ", QueryType.DELETE);
		}
	}

	public void executePhases() throws IOException, InterruptedException {
		loadOntologies();
		adjustRefDatasetsSizes();
		loadDatasets();
		generateCreativeWorks();
		loadCreativeWorks();
		setupAsynchronousAgents();
		warmUp();
		benchmark();
		stopAynchronousAgents();
		checkConformance();
		clearDatabase();
		
		System.out.println("END OF BENCHMARK RUN, all agents shut down...");
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		LoggingUtil.Configure();
		TestDriver testDriver = new TestDriver(args);
		testDriver.executePhases();
	}
}
