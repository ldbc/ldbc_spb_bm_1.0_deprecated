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
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query19.txt
 * A time range query template 
 */
public class Query19Template extends Query18Template {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query19.txt";
	
	private int creativeWorkType;
	
	public Query19Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(ru, queryTemplates, seedYear, substitutionParameters);
		this.substitutionParameters = substitutionParameters;
		preInitialize();
	}
	
	protected void preInitialize() {
		super.preInitialize();
		this.parameterIndex = 0;
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
	public String cwAudience() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		switch (creativeWorkType) {
		//cwork:BlogPost
		case 0 :
			return "cwork:InternationalAudience";
		//cwork:NewsItem
		case 1 :
			return "cwork:NationalAudience";
		//cwork:Programme
		case 2 :
			return "cwork:InternationalAudience";
		}
		return "cwork:InternationalAudience";		
	}	
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwType());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwAudience());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwFilterDateModifiedCondition());
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
