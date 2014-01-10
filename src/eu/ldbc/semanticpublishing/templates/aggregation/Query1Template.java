package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query1.txt
 */
public class Query1Template extends MustacheTemplate {

	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query1.txt"; 
	
	private final RandomUtil ru;
	
	public Query1Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAboutOrMentions}}}
	 */
	public String cwAboutOrMentions() {
		if (Definitions.aboutAndMentionsAllocation.getAllocation() == 0) {
			return "cwork:about";
		} else {
			return "cwork:mentions";
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAboutOrMentionsUri}}}
	 */
	public String cwAboutOrMentionsUri() {
		//use a popular or regular entity for about/mentions uri 
		boolean usePopularEntity = Definitions.usePopularEntities.getAllocation() == 0;
		
		Entity e;
		
		if (usePopularEntity) {
			e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
		} else {
			e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
		}
		
		return e.getURI();
	}

	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.CONSTRUCT;
	}	
}
