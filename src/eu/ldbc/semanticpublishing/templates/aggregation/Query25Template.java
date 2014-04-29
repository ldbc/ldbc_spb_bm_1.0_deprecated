package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query25.txt
 * A time-line of relatedness query
 */
public class Query25Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query25.txt";
	
	protected final RandomUtil ru;
	private String entityURI;
	
//	private boolean useCorrelatedEntitiesOnly = false;
	
	public Query25Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;		
		preInitialize();
	}	
	
	private void preInitialize() {
		this.entityURI = selectEntityURI();
//		this.useCorrelatedEntities = ru.nextBoolean();		
	}
	
	/**
	 * A method for replacing mustache template : {{{entityA}}}
	 */
	public String entityA() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
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
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(entityA());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(entityA());
			sb.append("\n");
			bw.write(sb.toString());
		}
		return null;
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
