package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query15.txt
 */
public class Query15Template  extends DefaultSelectTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query15.txt";

	private final int creativeWorkType;
	
	public Query15Template(HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */
	public String cwAudienceType() {
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
	public String getTemplateFileName() {
		return templateFileName;
	}

	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.SELECT;
	}
}
