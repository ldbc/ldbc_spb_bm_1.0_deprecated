package eu.ldbc.semanticpublishing.datagenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import eu.ldbc.semanticpublishing.datagenerator.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.SesameUtils;

/**
 * A class for generating Creative Works using components for serializing from the Sesame. 
 *
 */
public class GeneralWorker extends AbstractAsynchronousWorker {
	
	protected long targetTriples;
	protected long triplesPerFile;
	protected long totalTriplesForWorker;
	protected String destinationPath;
	protected String serializationFormat;
	protected AtomicLong filesCount;
	protected AtomicLong triplesGeneratedSoFar;
	protected RandomUtil ru;
	protected Object lock;
	
	public GeneralWorker(RandomUtil ru, Object lock, AtomicLong filesCount, long totalTriples, long triplesPerFile, AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat) {
		this.ru = ru;
		this.lock = lock;
		this.targetTriples = totalTriples;
		this.filesCount = filesCount;
		this.triplesPerFile = triplesPerFile;
		this.triplesGeneratedSoFar = triplesGeneratedSoFar;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
	}
	
	@Override
	public void execute() throws Exception {
		
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);
		
		FileUtils.makeDirectories(destinationPath);

		long currentFilesCount = filesCount.incrementAndGet();
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
		
		FileOutputStream fos = null;
		RDFWriter rdfWriter = null;
		
		int cwsInFileCount = 0;
		int currentTriplesCount = 0;
		
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		//loop until the generated triples have reached the targeted totalTriples size
		while (triplesGeneratedSoFar.get() < targetTriples) {
			try {
				fos = new FileOutputStream(fileName);
				rdfWriter = Rio.createWriter(rdfFormat, fos);
				rdfWriter.startRDF();

				while ( currentTriplesCount < triplesPerFile ) {
					Model sesameModel;
					
					//using a synchronized block, to guarantee the exactly equal generated data no matter the number of threads
					synchronized(lock) {							
						CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder("", ru);
						sesameModel = creativeWorkBuilder.buildSesameModel();
					}
				
					for (Statement statement : sesameModel) {
						rdfWriter.handleStatement(statement);
					}
						
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();
					triplesGeneratedSoFar.addAndGet(sesameModel.size());
					
					if (triplesGeneratedSoFar.get() > targetTriples) {					
						rdfWriter.endRDF();
						flushClose(fos); 
						System.out.println(Thread.currentThread().getName() + " GWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
						return;
					}
				}
				
				System.out.println(Thread.currentThread().getName() + " GWorker :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
				
				rdfWriter.endRDF();
				flushClose(fos);

				cwsInFileCount = 0;
				currentTriplesCount = 0;
								
				currentFilesCount = filesCount.getAndIncrement();
				fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);

				fos = new FileOutputStream(fileName);
				rdfWriter = Rio.createWriter(rdfFormat, fos);			
				rdfWriter.startRDF();
			} catch (RDFHandlerException e) {
				throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
			} finally {
				flushClose(fos);
			}
		}
	}
}
