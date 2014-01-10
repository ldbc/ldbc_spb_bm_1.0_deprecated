package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query8.txt
 */
public class Query8Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query8.txt";
	
	private static final int TIME_UNIT = Calendar.DAY_OF_YEAR;
	private static final int TIME_INTERVAL = 3;
	
	private final Date initialDate;
	private final int creativeWorkType;
	private final RandomUtil ru;	
	
	public Query8Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
		this.initialDate = ru.randomDateTime();
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwType}}}
	 */	
	public String cwType() {
		switch (creativeWorkType) {
		case 0 :
			return "cwork:BlogPost";
		case 1 :
			return "cwork:NewsItem";
		case 2 :
			return "cwork:Programme";
		}
		return "cwork:BlogPost";
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
	public String cwAudience() {
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