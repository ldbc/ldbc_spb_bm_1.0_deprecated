package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query21.txt
 */
public class Query21Template  extends DefaultSelectTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query21.txt";

	public Query21Template(HashMap<String, String> queryTemplates) {
		super(queryTemplates);
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
