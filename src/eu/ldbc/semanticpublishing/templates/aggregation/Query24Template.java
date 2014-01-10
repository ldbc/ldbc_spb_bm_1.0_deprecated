package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query24.txt
 * A geo-locations drill-down query template
 */
public class Query24Template extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query24.txt";
	//south boundary
	private static final double minLat = 50.45;	
	//north boundary
	private static final double maxLat = 53.25;
	//west boundary
	private static final double minLong = -2.15;
	//east boundary
	private static final double maxLong = 0.25;
	
	private final RandomUtil ru;
	
	private double referenceLat = 0.0;
	private double referenceLong = 0.0;
	//deviation value, sets the range by adding/subtracting it from referenceLat and referenceLong
	private double deviationValue = 0.25;
	
	public Query24Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.ru = ru;
		preInitialize();
	}
	
	private void preInitialize() {
		referenceLat = ru.nextDouble(minLat, maxLat);
		referenceLong = ru.nextDouble(minLong, maxLong);
		deviationValue = ru.nextDouble(0.20, 0.25);
	}
	
	public void initialize(double latitude, double longtitude, double deviationDecrease) {
		referenceLat = latitude;
		referenceLong = longtitude;
		deviationValue = ((deviationValue - deviationDecrease) > 0.0) ? (deviationValue - deviationDecrease) : 0.0 ;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLatitude}}}
	 */
	public String refLatitude() {
		return "" + referenceLat;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLongtitude}}}
	 */
	public String refLongtitude() {
		return "" + referenceLong;
	}
	
	/**
	 * A method for replacing mustache template : {{{refDeviation}}}
	 */
	public String refDeviation() {
		return "" + deviationValue;
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
