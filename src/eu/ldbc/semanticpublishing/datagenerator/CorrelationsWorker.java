package eu.ldbc.semanticpublishing.datagenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Model;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;

import eu.ldbc.semanticpublishing.datagenerator.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.SesameUtils;

/**
 * A class for generating Creative Works containing correlations between entities.
 * Currently implemented are correlations between two popular entities and a third entity.
 *
 */
public class CorrelationsWorker extends GeneralWorker {

	//first entity to participate in the correlation
	private Entity entityA;
	//second entity to participate in the correlation
	private Entity entityB;
	//third entity to participate in a sparse manner to the correlation - it will participate in a separate correlation separately with A and B and very sparsely with A and B during their overlap
	private Entity entityC;
	
	private int dataGenerationPeriodYears = 1;
	private int correlationsMagnitude = 10;
	private double correlationEntityLifespanPercent = 0.4;
	private double correlationDurationPercent = 0.1;
	
	private static final int THRID_ENTITY_CORRELATION_DISTANCE = 9;
	
	public CorrelationsWorker(RandomUtil ru, Entity entityA, Entity entityB, Entity entityC, int dataGenerationPeriodYears, int correlationsMagnitude, 
							  double correlationEntityLifespan, double correlationDuration, Object lock, AtomicLong filesCount, 
							  long totalTriples, long triplesPerFile, AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat) {
		super(ru, lock, filesCount, totalTriples, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat);
		this.entityA = entityA;
		this.entityB = entityB;
		this.entityC = entityC;
		this.dataGenerationPeriodYears = dataGenerationPeriodYears;
		this.correlationsMagnitude = correlationsMagnitude;
		this.correlationEntityLifespanPercent = correlationEntityLifespan;
		this.correlationDurationPercent = correlationDuration;
	}

	@Override
	public void execute() throws Exception {

//System.out.println(Thread.currentThread().getName() + " : A : " + entityA.getURI() + " : B : " + entityB.getURI() + " : C : " + entityC.getURI());		
		
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		FileOutputStream fos = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);
		FileUtils.makeDirectories(destinationPath);

		int cwsInFileCount = 0;
		int currentTriplesCount = 0;
		int thirdEntityCountdown = 0;
		long currentFilesCount = filesCount.incrementAndGet();		
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
				
		Date startDate;
		int thirdEntityInCorrelationOccurence = (int) ((365 * dataGenerationPeriodYears * correlationDurationPercent) / 10);
		int totalCorrelationPeriodDays = (int) (365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent * 2 - correlationDurationPercent));
		
		fos = new FileOutputStream(fileName);
		
		synchronized(lock) {
			//pick a random date starting from 1.Jan to the value of totalCorrelationPeriodDays
			startDate = ru.randomDateTime(365 * dataGenerationPeriodYears - totalCorrelationPeriodDays);
			thirdEntityCountdown = ru.nextInt((int)(THRID_ENTITY_CORRELATION_DISTANCE * 0.6), THRID_ENTITY_CORRELATION_DISTANCE);
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		try {
			for (int dayIncrement = 0; dayIncrement < totalCorrelationPeriodDays; dayIncrement++) {
				int correlationsMagnitude = ru.nextInt((int)(this.correlationsMagnitude * 0.75), this.correlationsMagnitude);				
				boolean thirdEntitySet = false;				
				
				//generate Creative Works with correlations for that day
				for (int i = 0; i < correlationsMagnitude; i++) {
					if (currentTriplesCount >= triplesPerFile) {						
						fos.close();
						System.out.println(Thread.currentThread().getName() + " CWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
	
						cwsInFileCount = 0;
						currentTriplesCount = 0;
						
						currentFilesCount = filesCount.incrementAndGet();
						fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
	
						fos = new FileOutputStream(fileName);										
					}
					
					if (triplesGeneratedSoFar.get() > targetTriples) {
						fos.close();
						System.out.println(Thread.currentThread().getName() + " CWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
						return;
					}
					
					Model sesameModel = null;
					
					synchronized(lock) {
						if (dayIncrement < 365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent - correlationDurationPercent)) {
							sesameModel = buildCreativeWork(entityA, entityC, null, true, calendar.getTime(), 0);
						} else if ((dayIncrement >= 365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent - correlationDurationPercent)) && dayIncrement < (365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent))) {
							//ru.nextBoolean() is used so that not all of third entity correlations to be set on each following day 
							if ((thirdEntityCountdown <= 0) && (thirdEntityInCorrelationOccurence > 0) && !thirdEntitySet) {
								//introduce a third entity correlation in a tiny amount of all correlations
								sesameModel = buildCreativeWork(entityA, entityB, entityC, true, calendar.getTime(), 0);
								thirdEntitySet = true;
								thirdEntityInCorrelationOccurence--;
								thirdEntityCountdown = ru.nextInt((int)(THRID_ENTITY_CORRELATION_DISTANCE * 0.6), THRID_ENTITY_CORRELATION_DISTANCE);
							} else {
								sesameModel = buildCreativeWork(entityA, entityB, null, true, calendar.getTime(), 0);
							}
						} else if (dayIncrement >= 365 * dataGenerationPeriodYears * correlationEntityLifespanPercent) {
							sesameModel = buildCreativeWork(entityB, entityC, null, true, calendar.getTime(), 0);
						} else {
							sesameModel = buildCreativeWork(entityA, entityC, null, true, calendar.getTime(), 0);
							System.out.println(Thread.currentThread().getName() + " :: Warning : Unexpected stage in data generation reached, defaulting");						
						}
					}
					
					Rio.write(sesameModel, fos, rdfFormat);
					
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();
					
					triplesGeneratedSoFar.addAndGet(sesameModel.size());				
				}
				thirdEntityCountdown--;
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}		
		} catch(RDFHandlerException e) {
			throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
		}		
	}
	
	private synchronized Model buildCreativeWork(Entity a, Entity b, Entity c, boolean aboutOrMentionsB, Date startDate, int dayIncrement) {
		CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder("", ru);
		creativeWorkBuilder.setDateIncrement(startDate, dayIncrement);
		creativeWorkBuilder.setAboutPresetUri(a.getURI());
		if (aboutOrMentionsB) {
			creativeWorkBuilder.setOptionalAboutPresetUri(b.getURI());
		} else {
			creativeWorkBuilder.setMentionsPresetUri(b.getURI());
		}
		if (c != null) {
			creativeWorkBuilder.setOptionalMentionsPresetUri(c.getURI());
		}
		creativeWorkBuilder.setUsePresetData(true);
		Model sesameModel = creativeWorkBuilder.buildSesameModel();
		return sesameModel;
	}
}