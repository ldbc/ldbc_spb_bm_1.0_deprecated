package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
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
	
	public Query23Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(ru, queryTemplates, definitions, substitutionParameters);		
	}
	
	@Override
	public void initialize(int iteration, String dateString, String[] substitutionParameters) {
		super.initialize(iteration, dateString, substitutionParameters);
	}
	
	/**
	 * A method for replacing mustache template : {{{projection}}}
	 */
	@Override
	public String projection() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
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
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}				
		
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
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}				
		
		if (iteration == 4) {
			return ORDER_BY_STRING;
		} else {
			return super.orderBy();
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
