package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query4.txt
 */
public class Query4Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query4.txt";
	
	private static final int TIME_INTERVAL_UNIT = Calendar.MONTH;
	private static final int TIME_INTERVAL = 1;
	
	private final RandomUtil ru;
	private final Date initialDate;
	
	public Query4Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
		
		//initial date will be one year from now, and randomly picked month between Jan and Apr
		this.initialDate = ru.randomDateTime(-1, 4);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwStartDateTime}}}
	 */		
	public String cwStartDateTime() {
		return ru.dateTimeString(initialDate);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwEndDateTime}}}
	 */
	public String cwEndDateTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(initialDate);
		calendar.add(TIME_INTERVAL_UNIT, TIME_INTERVAL);
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
