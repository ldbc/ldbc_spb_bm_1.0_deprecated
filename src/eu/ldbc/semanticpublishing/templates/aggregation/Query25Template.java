package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query25.txt
 * A time-line of relatedness query
 */
public class Query25Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query25.txt";
	
	protected final RandomUtil ru;
	private String entityURI;
	
//	private boolean useCorrelatedEntitiesOnly = false;
	
	public Query25Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
		this.entityURI = selectEntityURI();
//		this.useCorrelatedEntities = ru.nextBoolean();
	}	
	
	/**
	 * A method for replacing mustache template : {{{entityA}}}
	 */
	public String entityA() {
		return this.entityURI;
	}
	
	private String selectEntityURI() {
		if (DataManager.correlatedEntitiesList.size() > 0/* && useCorrelatedEntitiesOnly*/) {
			//correlatedEntitiesList contains URIs of correlated entities in the sequence : entityA1, entityB1, entityC1, entityA2, entityB2, entityC2...etc.			
			int position = ru.nextInt(DataManager.correlatedEntitiesList.size() / 3);
			//return 
			return DataManager.correlatedEntitiesList.get(position * 3);
		} else {
			return DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size())).getURI();
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
