package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query15.txt
 */
public class Query15Template  extends DefaultSelectTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query15.txt";

//	private final RandomUtil ru;
	
	private int creativeWorkType;
	
	private int parameterIndex;
	private final String[] substitutionParameters;	
	
	public Query15Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates);
		this.substitutionParameters = substitutionParameters;
		this.parameterIndex = 0;
		preInitialize();
	}
	
	private void preInitialize() {
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */
	public String cwAudienceType() {
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
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwAudienceType());
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
