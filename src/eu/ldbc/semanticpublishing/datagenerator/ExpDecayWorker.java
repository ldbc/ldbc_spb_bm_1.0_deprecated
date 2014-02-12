package eu.ldbc.semanticpublishing.datagenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import eu.ldbc.semanticpublishing.datagenerator.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.util.ExponentialDecayNumberGeneratorUtil;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.SesameUtils;

/**
 * Each worker will be responsible for generating data for a single "phenomenon" in datasets.
 * e.g. starts with a given date and using exponential decay will produce Creative Works which will tag in
 * their about tags certain entity (URI)
 *
 */
public class ExpDecayWorker extends GeneralWorker {
	private ExponentialDecayNumberGeneratorUtil expGenerator;
	private Date startDate;
	private String entityUriToTag;
	private int daySteps;
	
	public ExpDecayWorker(ExponentialDecayNumberGeneratorUtil expGenerator, Date startDate, String entityUriToUseForTagging, 
						  RandomUtil ru, Object lock, AtomicLong globalFilesCount, long triplesPerFile, long totalTriples, 
						  AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat) {
		super(ru, lock, globalFilesCount, totalTriples, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat);
		this.expGenerator = expGenerator;
		this.startDate = startDate;
		this.entityUriToTag = entityUriToUseForTagging;
		this.daySteps = 0;		
	}
	
	@Override
	public void execute() throws Exception {

		FileOutputStream fos = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);
		FileUtils.makeDirectories(destinationPath);

		int cwsInFileCount = 0;
		int currentTriplesCount = 0;
		boolean firstIteration = true;

		long currentFilesCount = filesCount.incrementAndGet();
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);

		RDFWriter rdfWriter = null;
		
		long creativeWorksForCurrentIteration = expGenerator.generateNext();		
		
		try {
			while (expGenerator.hasNext()) {				
				for (int i = 0; i < creativeWorksForCurrentIteration; i++) {
					if (firstIteration) {						
						fos = new FileOutputStream(fileName);
						rdfWriter = Rio.createWriter(rdfFormat, fos);						
						rdfWriter.startRDF();
						firstIteration = false;
					}
					
					if (currentTriplesCount >= triplesPerFile) {						
						rdfWriter.endRDF();
						flushClose(fos);
						System.out.println(Thread.currentThread().getName() + " :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far : " + triplesGeneratedSoFar.get() + ". Target: " + targetTriples + " triples");
	
						cwsInFileCount = 0;
						currentTriplesCount = 0;
						
						currentFilesCount = filesCount.incrementAndGet();
						fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
	
						fos = new FileOutputStream(fileName);
						rdfWriter = Rio.createWriter(rdfFormat, fos);			
						rdfWriter.startRDF();
					}
					
					if (triplesGeneratedSoFar.get() > targetTriples) {						
						rdfWriter.endRDF();
						flushClose(fos);
						return;
					}
					
					Model sesameModel;
					
					//using a synchronized block, to guarantee the exactly equal generated data no matter the number of threads
					synchronized(lock) {					
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(startDate);
						calendar.add(Calendar.DATE, daySteps);
						calendar.add(Calendar.HOUR, ru.nextInt(24));
						calendar.add(Calendar.MINUTE, ru.nextInt(60));
						calendar.add(Calendar.SECOND, ru.nextInt(60));
						calendar.add(Calendar.MILLISECOND, ru.nextInt(1000));
						CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder("", ru);
						creativeWorkBuilder.setPresetDate(calendar.getTime());
						creativeWorkBuilder.setUsePresetDate(true);
						creativeWorkBuilder.setPresetAboutTag(entityUriToTag);
						creativeWorkBuilder.setUsePresetAboutTag(true);
						sesameModel = creativeWorkBuilder.buildSesameModel();
					}
				
					for (Statement statement : sesameModel) {
						rdfWriter.handleStatement(statement);
					}
					
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();
					triplesGeneratedSoFar.addAndGet(sesameModel.size());	
				}
				daySteps++;
				creativeWorksForCurrentIteration = expGenerator.generateNext();
			}
		} catch (RDFHandlerException e) {
			throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
		} catch (NoSuchElementException nse) {
			//reached the end of iteration, close file stream
			if (rdfWriter != null) {
				rdfWriter.endRDF();
			}
		} finally {
			flushClose(fos);
		}
	}
}