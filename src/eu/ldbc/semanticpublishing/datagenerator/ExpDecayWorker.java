package eu.ldbc.semanticpublishing.datagenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Model;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;

import eu.ldbc.semanticpublishing.datagenerator.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
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
	private Entity entity;
	
	public ExpDecayWorker(ExponentialDecayNumberGeneratorUtil expGenerator, Date startDate, Entity entity, 
						  RandomUtil ru, Object lock, AtomicLong globalFilesCount, long triplesPerFile, long totalTriples, 
						  AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat) {
		super(ru, lock, globalFilesCount, totalTriples, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat);
		this.expGenerator = expGenerator;
		this.startDate = startDate;
		this.entity = entity;
	}
	
	@Override
	public void execute() throws Exception {

		FileOutputStream fos = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);
		FileUtils.makeDirectories(destinationPath);

		int cwsInFileCount = 0;
		int currentTriplesCount = 0;

		long currentFilesCount = filesCount.incrementAndGet();
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
				
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		long creativeWorksForCurrentIteration = 0;
		long iterationStep = 0;
		
		try {
			synchronized(lock) {
				creativeWorksForCurrentIteration = expGenerator.generateNext();
				iterationStep = expGenerator.getIterationStep();
			}			
			fos = new FileOutputStream(fileName);

			while (expGenerator.hasNext()) {
				for (int i = 0; i < creativeWorksForCurrentIteration; i++) {
					if (currentTriplesCount >= triplesPerFile) {
						flushClose(fos);
						System.out.println(Thread.currentThread().getName() + " EWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
							
						cwsInFileCount = 0;
						currentTriplesCount = 0;				

						currentFilesCount = filesCount.incrementAndGet();
						fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);						
						
						fos = new FileOutputStream(fileName);
					}
					
					if (triplesGeneratedSoFar.get() > targetTriples) {
						return;
					}
					
					Model sesameModel;
					
					//using a synchronized block, to guarantee the exactly equal generated data no matter the number of threads
					synchronized(lock) {		
						CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder("", ru);
						creativeWorkBuilder.setDateIncrement(startDate, (int)iterationStep);
						creativeWorkBuilder.setAboutPresetUri(entity.getURI());
						creativeWorkBuilder.setUsePresetData(true);
						sesameModel = creativeWorkBuilder.buildSesameModel();						
					}
					
					Rio.write(sesameModel, fos, rdfFormat);
					
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();					

					triplesGeneratedSoFar.addAndGet(sesameModel.size());					
				}

				synchronized(lock) {
					creativeWorksForCurrentIteration = expGenerator.generateNext();
					iterationStep = expGenerator.getIterationStep();
				}
			}
		} catch (RDFHandlerException e) {
			throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
		} catch (NoSuchElementException nse) {
			//reached the end of iteration, close file stream in finally section
		} finally {
			flushClose(fos);
			System.out.println(Thread.currentThread().getName() + " EWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");			
		}
	}
}