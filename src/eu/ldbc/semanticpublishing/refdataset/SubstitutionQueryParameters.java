package eu.ldbc.semanticpublishing.refdataset;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;

public class SubstitutionQueryParameters {
	private String queryName;
	private List<String> substParametersList;
	
	public SubstitutionQueryParameters(String queryName) {
		this.queryName = queryName;
		this.substParametersList = new ArrayList<String>();
	}
	
	public void initFromFile(String fullPath) throws IOException, InterruptedException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fullPath));
			String line = br.readLine();
			while (line != null) {
				substParametersList.add(line);
				line = br.readLine();
			}
		} catch (IOException ioe) {
			System.out.println("\tFailed to initialize query substitution parameters from : " + fullPath + ", will use random values for query parameters. Set generateQuerySubstitutionParameters=true to enable.");
		} finally {
			try { br.close();} catch(Exception e) {}
		}
	}
	
	public String getQueryName() {
		return this.queryName;
	}

	public String[] get(long ind) {
		if (substParametersList.size() > 0) {
			if (ind >= substParametersList.size()) {
				return substParametersList.get((int)ind % substParametersList.size()).split(QueryParametersGenerator.PARAMS_DELIMITER);
			} else {
				return substParametersList.get((int)ind).split(QueryParametersGenerator.PARAMS_DELIMITER);
			}
		}
		return null;		
	}
}
