package eu.ldbc.semanticpublishing.validation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParameters;

public class ValidationValues {
	private static final String RESULTS_HEADER = "[Results]";
	
	private SubstitutionQueryParameters substitutionParameters;
	private String queryName;
	private List<String> validationResultsList;
	
	public ValidationValues(String queryName) {
		this.queryName = queryName;
		substitutionParameters = new SubstitutionQueryParameters(queryName);
		validationResultsList = new ArrayList<String>();
	}
	
	public void initFromFile(String fullPath, boolean suppressErrorMessages) throws IOException, InterruptedException {
		substitutionParameters.initFromFile(fullPath, suppressErrorMessages, true);
		
		BufferedReader br = null;
		try {
			boolean canAdd = false;
			br = new BufferedReader(new FileReader(fullPath));
			String line = br.readLine();
			while (line != null) {	
				if (canAdd) {
					validationResultsList.add(line);
				}
				
				if (line.contains(RESULTS_HEADER)) {
					canAdd = true;
				}
				
				line = br.readLine();
			}
		} catch (IOException ioe) {
			if (!suppressErrorMessages) {
				System.out.println("\tFailed to initialize validation results from : " + fullPath);
			}
		} finally {
			try { br.close();} catch(Exception e) {}
		}
	}
	
	public String getQueryName() {
		return this.queryName;
	}
	
	public String[] getSubstitutionParameters() {
		return substitutionParameters.get(0);
	}
	
	public List<String> getValidationResultsList() {
		return validationResultsList;
	}
}
