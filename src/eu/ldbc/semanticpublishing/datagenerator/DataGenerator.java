package eu.ldbc.semanticpublishing.datagenerator;

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
	private long totalTriples;
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
		this.totalTriples = totalTriples;
		this.triplesPerFile = triplesPerFile;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.workersSyncLock = new Object();
	}
	
	public void produceData() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(generatorThreads);
		long creativeWorksInDatabase = DataManager.creativeWorksNexId.get();
		
		if (creativeWorksInDatabase > 0) {
			System.out.println("\t" + creativeWorksInDatabase + " Creative Works currently exist.");
		}
		
		long currentTime = System.currentTimeMillis();

		//Generate MAJOR EVENTS with exponential decay
		ExponentialDecayNumberGeneratorUtil edgu;

		int exponentialDecayUpperLimitOfCws = definitions.getInt(Definitions.EXPONENTIAL_DECAY_UPPER_LIMIT_OF_CWS);
		
		for (int i = 0; i < definitions.getInt(Definitions.MAJOR_EVENTS_PER_YEAR); i++) {
			edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000, */exponentialDecayUpperLimitOfCws, 
						  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
						  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
			Entity e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			executorService.execute(new ExpDecayWorker(edgu, ru.randomDateTime(), e.getURI(), ru, workersSyncLock, filesCount, 
													   triplesPerFile, totalTriples, triplesGeneratedSoFar, destinationPath, serializationFormat));
		}
		executorService.shutdown();
		executorService.awaitTermination(AWAIT_PERIOD_HOURS, TimeUnit.HOURS);
		
		//Generate MINOR EVENTS with exponential decay
		executorService = Executors.newFixedThreadPool(generatorThreads);		
		
		for (int i = 0; i < definitions.getInt(Definitions.MINOR_EVENT_PER_YEAR); i++) {
			edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000,*/ exponentialDecayUpperLimitOfCws / 10, 
						  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
						  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
			Entity e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
			executorService.execute(new ExpDecayWorker(edgu, ru.randomDateTime(), e.getURI(), ru, workersSyncLock, filesCount, 
														triplesPerFile, totalTriples, triplesGeneratedSoFar, destinationPath, serializationFormat));
		}
		
		executorService.shutdown();
		executorService.awaitTermination(AWAIT_PERIOD_HOURS, TimeUnit.HOURS);
		
		//Generate noise to fill-in with generated data with randomly distributed tags of creative works, i.e. model "noise"
		executorService = Executors.newFixedThreadPool(generatorThreads);
		
		if (triplesGeneratedSoFar.get() < totalTriples) {
			for (int i = 0; i < configuration.getInt(Configuration.DATA_GENERATOR_WORKERS); i++) {
				executorService.execute(new GeneralWorker(ru, workersSyncLock, filesCount, totalTriples, 
														  triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat));			
			}
		}
		
		executorService.shutdown();
		executorService.awaitTermination(AWAIT_PERIOD_HOURS, TimeUnit.HOURS);
		
		System.out.println("\tcompleted! Total Creative Works created : " + String.format("%,d", (DataManager.creativeWorksNexId.get() - creativeWorksInDatabase)) + " in " + filesCount.get() + " files. Time : " + (System.currentTimeMillis() - currentTime) + " ms");
	}
}
