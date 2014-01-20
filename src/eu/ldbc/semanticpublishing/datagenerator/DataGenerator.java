package eu.ldbc.semanticpublishing.datagenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.ThreadUtil;

/**
 * The class responsible for managing data generation for the benchmark.
 * It is the entry point for any data generation related process.  
 *
 */
public class DataGenerator {
	private RandomUtil ru;
	private int generatorThreads = 1;
	private MustacheTemplatesHolder mustacheTemplatesHolder;
	private long triplesPerFile;
	private long totalTriples;
	private AtomicLong filesCount = new AtomicLong(0);
	private AtomicLong triplesGeneratedSoFar = new AtomicLong(0);
	private String destinationPath;
	private String serializationFormat;
	private final List<DataGeneratorWorker> generatorThreadsList = new ArrayList<DataGeneratorWorker>();
	
	protected Object workersSyncLock;
	
	public DataGenerator(RandomUtil ru, Configuration configuration, MustacheTemplatesHolder mustacheTemplatesHolder, int generatorThreads, long totalTriples, long triplesPerFile, String destinationPath, String serializationFormat) {
		this.ru = ru;
		this.mustacheTemplatesHolder = mustacheTemplatesHolder;
		this.generatorThreads = generatorThreads;
		this.totalTriples = totalTriples;
		this.triplesPerFile = triplesPerFile;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.workersSyncLock = new Object();
		setupGeneratorThreads();
	}
	
	private void setupGeneratorThreads() {
		if (generatorThreads <= 0) {
			System.out.println("DataGenerator : wrong number of generator threads : " + generatorThreads + ", check value of property : \"dataGeneratorWorkers\" in test.properties file!");
			return;
		}
		
		long triplesPerThread = totalTriples / generatorThreads;
		
		for (int i = 0; i < generatorThreads; i++) {
			generatorThreadsList.add(new DataGeneratorWorker(ru, workersSyncLock, filesCount, totalTriples, triplesPerThread, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL)));
		}
	}
	
	public void produceData() {		
		long creativeWorksInDatabase = DataManager.creativeWorksNexId.get();
		
		if (creativeWorksInDatabase > 0) {
			System.out.println("\t" + creativeWorksInDatabase + " Creative Works currently exist.");
		}
		
		if (generatorThreadsList.size() == 0) {
			return;
		}
		
		long currentTime = System.currentTimeMillis();
		
		//start the threads
		for (DataGeneratorWorker worker : generatorThreadsList) {
			worker.start();
		}
		
		//... and wait for all threads to complete
		for (DataGeneratorWorker generatorThread : generatorThreadsList) {
			ThreadUtil.join(generatorThread);

		}
		System.out.println("\tcompleted! Total Creative Works created : " + (DataManager.creativeWorksNexId.get() - creativeWorksInDatabase) + " in " + filesCount.get() + " files. Time : " + (System.currentTimeMillis() - currentTime) + " ms");
	}
}
