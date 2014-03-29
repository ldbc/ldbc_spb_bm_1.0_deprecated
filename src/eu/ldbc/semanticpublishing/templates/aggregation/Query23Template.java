package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query23.txt
 * A time faceted search query template. Query23 deviates from Query21 by the query executed during last iteration 
 */
public class Query23Template extends Query21Template {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query23.txt";
	
	protected static final String PROJECTION_STRING = "?day ?tag ((COUNT(*)) AS ?count)";
	
	protected static final String GROUP_BY_STRING = "GROUP BY ?day ?tag";
	protected static final String ORDER_BY_STRING = "ORDER BY ?day ?tag";
	
	public Query23Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(ru, queryTemplates);
	}
	
	/**
	 * A method for replacing mustache template : {{{projection}}}
	 */
	@Override
	public String projection() {
		if (iteration == 4) {
			return PROJECTION_STRING;
		} else {
			return super.projection();
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{groupBy}}}
	 */
	@Override
	public String groupBy() {
		if (iteration == 4) {
			return GROUP_BY_STRING;
		} else {
			return super.groupBy();
		}
	}	

	/**
	 * A method for replacing mustache template : {{{orderBy}}}
	 */
	@Override
	public String orderBy() {
		if (iteration == 4) {
			return ORDER_BY_STRING;
		} else {
			return super.orderBy();
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
