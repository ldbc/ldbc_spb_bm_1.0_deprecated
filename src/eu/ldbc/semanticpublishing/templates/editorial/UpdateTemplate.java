package eu.ldbc.semanticpublishing.templates.editorial;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class implementing the MustacheTemplateCompiler, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/editorial/update.txt
 */
public class UpdateTemplate extends InsertTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "update.txt";
	
	public UpdateTemplate(String contextURI, RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(contextURI, ru, queryTemplates);
	}
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.UPDATE;
	}
}
