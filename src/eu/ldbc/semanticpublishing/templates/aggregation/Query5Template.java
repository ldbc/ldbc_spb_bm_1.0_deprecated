package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query5.txt
 */
public class Query5Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query5.txt";	

	private final RandomUtil ru;
	
	private static final String[] categoryTypes = {	"<http://www.bbc.co.uk/category/PoliticsPersonsReference>", 
													"<http://www.bbc.co.uk/category/PoliticsPersons>", 
													"<http://www.bbc.co.uk/category/PoliticsPersonsAdditional>", 
													"<http://www.bbc.co.uk/category/SportsTeams>", 
													"<http://www.bbc.co.uk/category/SportsCompetitions>"
	};
	
	private static final String[] entityTypes = {"sport:RecurringCompetition",
												 "sport:Competition",
												 "sport:CompetitiveSportingOrganisation",
												 "news:Person"
	};

	public Query5Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAboutEntityType}}}
	 */	
	public String cwAboutEntityType() {
		return entityTypes[ru.nextInt(entityTypes.length)];
	}

	/**
	 * A method for replacing mustache template : {{{cwCategoryType}}}
	 */		
	public String cwCategoryType() {
		return categoryTypes[ru.nextInt(categoryTypes.length)];
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
