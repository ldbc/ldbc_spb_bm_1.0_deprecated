package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query13.txt
 */
public class Query13Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query13.txt";
	
	private final RandomUtil ru;
	
	private static final String[] categoryTypes = {	"<http://www.bbc.co.uk/category/PoliticsPersonsReference>", 
													"<http://www.bbc.co.uk/category/PoliticsPersons>", 
													"<http://www.bbc.co.uk/category/PoliticsPersonsAdditional>", 
													"<http://www.bbc.co.uk/category/SportsTeams>", 
													"<http://www.bbc.co.uk/category/SportsCompetitions>"
   };

	public Query13Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;		
	}
	
	/**
	 * A method for replacing mustache template : {{{cwCategoryType}}}
	 */	
	public String cwCategoryType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return categoryTypes[ru.nextInt(categoryTypes.length)];
	}
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			sb.setLength(0);
			sb.append(cwCategoryType());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwCategoryType());
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
