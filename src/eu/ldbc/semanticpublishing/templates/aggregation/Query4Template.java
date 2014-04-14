package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query4.txt
 */
public class Query4Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query4.txt";
	
	private static final int TIME_INTERVAL_UNIT = Calendar.MONTH;
	private static final int TIME_INTERVAL_MONTHS = 8;// 2/3 of a whole year
	
	private final RandomUtil ru;
	private Date initialDate;
	
	public Query4Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		preInitialize();
	}
	
	private void preInitialize() {
		this.initialDate = ru.randomDateTime(-1, 12);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwStartDateTime}}}
	 */		
	public String cwStartDateTime() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.dateTimeString(initialDate);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwEndDateTime}}}
	 */
	public String cwEndDateTime() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(initialDate);
		calendar.add(TIME_INTERVAL_UNIT, TIME_INTERVAL_MONTHS);
		return ru.dateTimeString(calendar.getTime());
	}	

	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwStartDateTime());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwEndDateTime());
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
