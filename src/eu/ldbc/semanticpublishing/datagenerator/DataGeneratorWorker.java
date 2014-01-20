package eu.ldbc.semanticpublishing.datagenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import eu.ldbc.semanticpublishing.templates.editorial.InsertTemplate;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class for generating Creative Works using components for serializing from the Sesame. 
 *
 */
public class DataGeneratorWorker extends AbstractAsynchronousWorker {

	private static final String FILENAME_FORMAT = "%s%sgeneratedCreativeWorks-%04d.";
	
	private long totalTriples;
	private long triplesPerFile;
	private long totalTriplesForWorker;
	private String destinationPath;
	private String serializationFormat;
	private AtomicLong filesCount;
	private AtomicLong triplesGeneratedSoFar;
	private RandomUtil ru;
	private HashMap<String, String> editorialTemplates;
	private Object lock;
	
	public DataGeneratorWorker(RandomUtil ru, Object lock, AtomicLong filesCount, long totalTriples, long totalTriplesPerThread, long triplesPerFile, AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat, HashMap<String, String> editorialTemplates) {
		this.ru = ru;
		this.lock = lock;
		this.totalTriples = totalTriples;
		this.filesCount = filesCount;
		this.totalTriplesForWorker = totalTriplesPerThread;
		this.triplesPerFile = triplesPerFile;
		this.triplesGeneratedSoFar = triplesGeneratedSoFar;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.editorialTemplates = editorialTemplates;
	}
	
	@Override
	public void execute() throws IOException {
		RDFFormat rdfFormat = RDFFormat.NQUADS;
		
		if (serializationFormat.equalsIgnoreCase("BinaryRDF")) {
			rdfFormat = RDFFormat.BINARY;
		} else if (serializationFormat.equalsIgnoreCase("TriG")) {
			rdfFormat = RDFFormat.TRIG;
		} else if (serializationFormat.equalsIgnoreCase("TriX")) {
			rdfFormat = RDFFormat.TRIX;
		} else if (serializationFormat.equalsIgnoreCase("N-Triples")) {
			rdfFormat = RDFFormat.NTRIPLES;
		} else if (serializationFormat.equalsIgnoreCase("N-Quads")) {	
			rdfFormat = RDFFormat.NQUADS;
		} else if (serializationFormat.equalsIgnoreCase("N3")) {
			rdfFormat = RDFFormat.N3;
		} else if (serializationFormat.equalsIgnoreCase("RDF/XML")) {
			rdfFormat = RDFFormat.RDFXML;
		} else if (serializationFormat.equalsIgnoreCase("RDF/JSON")) {
			rdfFormat = RDFFormat.RDFJSON;
		} else if (serializationFormat.equalsIgnoreCase("Turtle")) {
			rdfFormat = RDFFormat.TURTLE;
		} else {
			throw new IllegalArgumentException("Warning : unknown serialization format : " + serializationFormat + ", defaulting to N-Quads");
		}
		
		FileUtils.makeDirectories(destinationPath);

		//loop until the maximum number of triples to store in the database is reached
		for (long triplesCount = 0; triplesCount < totalTriplesForWorker; ) {

			//Adjust the number of triples in last batch
			long triplesLeftInFile = triplesPerFile;
			if ((totalTriplesForWorker - triplesCount) >= triplesPerFile) {
				triplesLeftInFile = triplesPerFile;
			} else {
				triplesLeftInFile = totalTriplesForWorker - triplesCount;
			}
			
			long currentFilesCount = filesCount.incrementAndGet();
			String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
			
			FileOutputStream fos = null;			

			try {
				fos = new FileOutputStream(fileName);
				RDFWriter rdfWriter = Rio.createWriter(rdfFormat, fos);
				
				rdfWriter.startRDF();
	
				int cwsInFileCount = 0;
				int currentTriplesCount = 0;
				while ( currentTriplesCount < triplesLeftInFile ) {
					Model sesameModel;
					
					//using a synchronized block, to guarantee the exactly equal generated data no matter the number of threads
					synchronized(lock) {							
						InsertTemplate insertQuery = new InsertTemplate("", ru, editorialTemplates); 
						sesameModel = insertQuery.buildSesameModel();
					}
				
					for (Statement statement : sesameModel) {
						rdfWriter.handleStatement(statement);
					}
						
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();
				}
				triplesCount += currentTriplesCount;
				triplesGeneratedSoFar.addAndGet(currentTriplesCount);
				
				rdfWriter.endRDF();

				System.out.println(Thread.currentThread().getName() + " :: Saving file #" + currentFilesCount + " with " + cwsInFileCount + " Creative Works. Generated triples so far : " + triplesGeneratedSoFar.get() + ". Target: " + totalTriples + " triples");
			} catch (RDFHandlerException e) {
				throw new IOException("A problem occurred generating RDF data: " + e.getMessage());
			} finally {
				if(fos != null) {
					fos.close();
				}
			}							
		}
	}
}
