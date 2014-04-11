package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query22.txt
 * A time faceted search query template. Query22 deviates from Query21 by the query executed during last iteration 
 */
public class Query22Template extends Query21Template {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query22.txt";

	protected static final String FILTER_DATE_YM_STRING = "FILTER (?year = %s && ?month = %s) .";
	
	public Query22Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(ru, queryTemplates, seedYear, substitutionParameters);
	}
	
	/**
	 * @param dateString - expected date string format : 2010-10-02T17:41:36.229+03:00
	 */
	@Override
	public void initialize(int iteration, String dateString, String[] substitutionParameters) {
		this.iteration = iteration;
		this.substitutionParameters = substitutionParameters;
		
		try {
			if (!dateString.isEmpty()) {			
				String tokens[] = dateString.split("-");
				if (tokens.length == 3) {
					this.year = Integer.parseInt(tokens[0]);
					this.month = Integer.parseInt(tokens[1]);
				}
			}
		} catch (NumberFormatException nfe) {
			//sink the exception
		}
	}	
	
	/**
	 * A method for replacing mustache template : {{{filter3}}}
	 */
	@Override
	public String filter3() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		if (iteration == 4) {
			return String.format(FILTER_DATE_YM_STRING, year, month);
		} else {
			return " ";
		}
	}
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			super.preInitialize();
			sb.setLength(0);
			sb.append(projection());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(filter1());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(filter2());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(filter3());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(groupBy());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(orderBy());
			sb.append("\n");
			bw.write(sb.toString());	
		}
	}	
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.SELECT;
	}		
}
