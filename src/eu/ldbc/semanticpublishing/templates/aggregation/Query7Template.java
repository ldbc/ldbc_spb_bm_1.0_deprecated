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
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query7.txt
 */
public class Query7Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query7.txt";
	
	private static final int PRIMARY_CONTENT_COUNT_MAX = 4;
	
	private final RandomUtil ru;
	
	public Query7Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{filterCondition}}}
	 */		
	public String filterCondition() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return String.format("FILTER(?pcCount > %d) ", ru.nextInt(1, PRIMARY_CONTENT_COUNT_MAX));
	}
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			sb.setLength(0);
			sb.append(filterCondition());
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
