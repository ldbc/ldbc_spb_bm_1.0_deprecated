package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
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
	
	public Query22Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(ru, queryTemplates);
	}
	
	/**
	 * @param dateString - expected date string format : 2010-10-02T17:41:36.229+03:00
	 */
	@Override
	public void initialize(int iteration, String dateString) {
		this.iteration = iteration;
		
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
		if (iteration == 4) {
			return String.format(FILTER_DATE_YM_STRING, year, month);
		} else {
			return "";
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
