package eu.ldbc.semanticpublishing.templates.editorial;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/editorial/delete.txt
 */
public class DeleteTemplate extends MustacheTemplate {

	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "delete.txt"; 
	
	private final RandomUtil ru;
	
	public DeleteTemplate(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwGraphUri}}}
	 */	
	public String cwGraphUri() {
		long cwNextId = ru.nextInt((int)DataManager.creativeWorksNexId.get());
		return ru.numberURI("context", cwNextId, true, true);
	}

	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}

	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.DELETE;
	}
}
