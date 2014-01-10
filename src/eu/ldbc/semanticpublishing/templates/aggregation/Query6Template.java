package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query6.txt
 */
public class Query6Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query6.txt";
	
	private final RandomUtil ru;
	
	public Query6Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwLiveCoverage}}}
	 */	
	public String cwLiveCoverage() {
		return ru.createBoolean(ru.nextBoolean());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
	public String cwAudience() {
		if (ru.nextBoolean()) {
			return "cwork:NationalAudience";
		} else {
			return "cwork:InternationalAudience";
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
