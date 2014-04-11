package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query3.txt
 */
public class Query3Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query3.txt"; 
	
	private final RandomUtil ru;
	private Date initialDate;
	
	private static final int TIME_UNIT = Calendar.HOUR;
	private static final int TIME_INTERVAL = 1;
	
	public Query3Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;		
		preInitialize();
	}
	
	private void preInitialize() {
		this.initialDate = ru.randomDateTime();
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
		calendar.add(TIME_UNIT, TIME_INTERVAL);
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
