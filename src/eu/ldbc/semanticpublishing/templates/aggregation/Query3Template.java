package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query3.txt
 */
public class Query3Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query3.txt"; 
	
	private final RandomUtil ru;
	private final Date initialDate;
	
	private static final int TIME_UNIT = Calendar.HOUR;
	private static final int TIME_INTERVAL = 1;
	
	public Query3Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
		this.initialDate = ru.randomDateTime();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwStartDataTime}}}
	 */		
	public String cwStartDataTime() {
		return ru.dateTimeString(initialDate);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwEndDateTime}}}
	 */
	public String cwEndDateTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(initialDate);
		calendar.add(TIME_UNIT, TIME_INTERVAL);
		return ru.dateTimeString(calendar.getTime());
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
