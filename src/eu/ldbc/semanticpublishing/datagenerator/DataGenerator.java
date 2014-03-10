package eu.ldbc.semanticpublishing.datagenerator;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.ExponentialDecayNumberGeneratorUtil;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * The class responsible for managing data generation for the benchmark.
 * It is the entry point for any data generation related process.  
 *
 */
public class DataGenerator {
	private RandomUtil ru;
	private Configuration configuration;
	private Definitions definitions;
	private int generatorThreads = 1;
	private long triplesPerFile;
	private long targetedTriplesSize;
	private AtomicLong filesCount = new AtomicLong(0);
	private AtomicLong triplesGeneratedSoFar = new AtomicLong(0);
	private String destinationPath;
	private String serializationFormat;
	private static final long AWAIT_PERIOD_HOURS = 6; 
	
	protected Object workersSyncLock;
	
	public DataGenerator(RandomUtil ru, Configuration configuration, Definitions definitions, int generatorThreads, long totalTriples, long triplesPerFile, String destinationPath, String serializationFormat) {
		this.ru = ru;
		this.configuration = configuration;
		this.definitions = definitions;
		this.generatorThreads = generatorThreads;
		this.targetedTriplesSize = totalTriples;
		this.triplesPerFile = triplesPerFile;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.workersSyncLock = new Object();
	}
	
	public void produceData() throws InterruptedException {
		long creativeWorksInDatabase = DataManager.creativeWorksNexId.get();
		
		if (creativeWorksInDatabase > 0) {
			System.out.println("\t" + creativeWorksInDatabase + " Creative Works currently exist.");
		}
		
		long currentTime = System.currentTimeMillis();

		//Generate MAJOR EVENTS with exponential decay
		ExponentialDecayNumberGeneratorUtil edgu;

		int exponentialDecayUpperLimitOfCws = definitions.getInt(Definitions.EXPONENTIAL_DECAY_UPPER_LIMIT_OF_CWS);

		ExecutorService executorService = null;
		executorService = Executors.newFixedThreadPool(generatorThreads);
		if (definitions.getInt(Definitions.MAJOR_EVENTS_PER_YEAR) > 0) {

			for (int i = 0; i < definitions.getInt(Definitions.MAJOR_EVENTS_PER_YEAR); i++) {
				edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000, */exponentialDecayUpperLimitOfCws, 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
				Date startDate = ru.randomDateTime();
				Entity e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
				for (int j = 0; j < generatorThreads; j++) {
					ExpDecayWorker edw = new ExpDecayWorker(edgu, startDate, e, ru, workersSyncLock, filesCount, triplesPerFile, targetedTriplesSize, triplesGeneratedSoFar, destinationPath, serializationFormat);
					executorService.execute(edw);				
				}
			}
		}
		
		if (triplesGeneratedSoFar.get() >= targetedTriplesSize) {
			System.out.println("Generated triples abount (" + triplesGeneratedSoFar.get() + ") has reached targeted triples size (" + targetedTriplesSize + "), stopping generation...");
			return;
		}
		
		//Generate MINOR EVENTS with exponential decay
		if (definitions.getInt(Definitions.MINOR_EVENT_PER_YEAR) > 0) {			
			for (int i = 0; i < definitions.getInt(Definitions.MINOR_EVENT_PER_YEAR); i++) {
				edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000,*/ exponentialDecayUpperLimitOfCws / 10, 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
				Date startDate = ru.randomDateTime();
				Entity e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
				for (int j = 0; j < generatorThreads; j++) {			
					ExpDecayWorker edw = new ExpDecayWorker(edgu, startDate, e, ru, workersSyncLock, filesCount, triplesPerFile, targetedTriplesSize, triplesGeneratedSoFar, destinationPath, serializationFormat);
					executorService.execute(edw);
				}
			}
		}
				
		//Generate Correlations between entities
		int correlationsAmount = definitions.getInt(Definitions.CORRELATIONS_AMOUNT);
		if (correlationsAmount > 0) {
			LinkedList<Entity> entitiesList = buildCorrelationsList(correlationsAmount);		
				
			while (!entitiesList.isEmpty()) {				
				CorrelationsWorker cw = new CorrelationsWorker(ru, entitiesList.remove(), entitiesList.remove(), entitiesList.remove(), definitions.getInt(Definitions.DATA_GENERATOR_PERIOD_YEARS), 
															   definitions.getInt(Definitions.CORRELATIONS_MAGNITUDE), definitions.getDouble(Definitions.CORRELATION_ENTITY_LIFESPAN), 
															   definitions.getDouble(Definitions.CORRELATIONS_DURATION), workersSyncLock, filesCount, targetedTriplesSize, triplesPerFile, 
															   triplesGeneratedSoFar, destinationPath, serializationFormat);
				executorService.execute(cw);
			}
		}
		
		//Generate random Creative Works to fill-in with rest of the generated data with randomly distributed tags of creative works, i.e. generate "noise"
		if ((triplesGeneratedSoFar.get() < targetedTriplesSize) && configuration.getBoolean(Configuration.USE_GENERAL_DATA_GENERATORS)) {
			for (int i = 0; i < configuration.getInt(Configuration.DATA_GENERATOR_WORKERS); i++) {				
				GeneralWorker gw = new GeneralWorker(ru, workersSyncLock, filesCount, targetedTriplesSize, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat);
				executorService.execute(gw);
			}
		}		
		executorService.shutdown();
		executorService.awaitTermination(AWAIT_PERIOD_HOURS, TimeUnit.HOURS);		
		
		System.out.println("\tcompleted! Total Creative Works created : " + String.format("%,d", (DataManager.creativeWorksNexId.get() - creativeWorksInDatabase)) + " in " + filesCount.get() + " files. Time : " + (System.currentTimeMillis() - currentTime) + " ms");
	}
	
	private LinkedList<Entity> buildCorrelationsList(int correlationsAmount) {
		LinkedList<Entity> linkedList = new LinkedList<Entity>();
		
		for (int i = 0; i < correlationsAmount; i++) {
			Entity e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			linkedList.add(e);
			e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			linkedList.add(e);
			e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
			linkedList.add(e);			
		}
		
		return linkedList;
	}
}
