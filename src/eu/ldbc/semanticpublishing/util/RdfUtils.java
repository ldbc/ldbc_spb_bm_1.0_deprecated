package eu.ldbc.semanticpublishing.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for configuring the POST http request with different serialization content types.
 */
public class RdfUtils {
	
	private static final int READ_BUFFER_SIZE_BYTES = 128 * 1024;
	
	public static final String CONTENT_TYPE_NQUADS = "application/n-quads";
	public static final String CONTENT_TYPE_SESAME_NQUADS = "text/x-nquads";
	public static final String CONTENT_TYPE_TRIG = "application/x-trig";
	public static final String CONTENT_TYPE_TURTLE = "application/x-turtle";

	public static void postStatements(String endpoint, String contentType, InputStream input) throws IOException {
		
		URL url = new URL(endpoint);
		HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();
		httpUrlConnection.setDefaultUseCaches(false);
		httpUrlConnection.setUseCaches(false);
		httpUrlConnection.setDoOutput(input != null);

		httpUrlConnection.setRequestMethod("POST");
		httpUrlConnection.setRequestProperty("Content-Type", contentType);
//		httpUrlConnection.setRequestProperty("Accept", "*/*");

		if(input != null) {
			OutputStream outStream = httpUrlConnection.getOutputStream();
			
			try {
				int b; 
				byte[] buffer = new byte[READ_BUFFER_SIZE_BYTES];
				while((b = input.read(buffer)) >= 0) {
					outStream.write(buffer, 0, b);
				}
				outStream.flush();
			}
			finally {
				input.close();
				outStream.close();
			}
		}
		
		int code = httpUrlConnection.getResponseCode();
		if (code < 200 || code >= 300) {
			throw new IOException("Posting statements received error code : " + code + " from server.");
		}
		
		httpUrlConnection.getInputStream().close();
	}
}
