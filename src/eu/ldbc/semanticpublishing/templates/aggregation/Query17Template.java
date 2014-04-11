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
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query17.txt
 * A geo-locations drill-down query template
 */
public class Query17Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query17.txt";
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
	
	public Query17Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;	
		preInitialize();
	}
	
	private void preInitialize() {
		referenceLat = ru.nextDouble(minLat, maxLat);
		referenceLong = ru.nextDouble(minLong, maxLong);
		deviationValue = ru.nextDouble(0.20, 0.25);
		parameterIndex = 0;
	}
	
	public void initialize(double latitude, double longtitude, double deviationDecrease, String[] substitutionParameters) {
		this.referenceLat = latitude;
		this.referenceLong = longtitude;
		this.deviationValue = ((deviationValue - deviationDecrease) > 0.0) ? (deviationValue - deviationDecrease) : 0.0 ;
		this.substitutionParameters = substitutionParameters;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLatitude}}}
	 */
	public String refLatitude() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "" + referenceLat;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLongtitude}}}
	 */
	public String refLongtitude() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "" + referenceLong;
	}
	
	/**
	 * A method for replacing mustache template : {{{refDeviation}}}
	 */
	public String refDeviation() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return "" + deviationValue;
	}
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(refLatitude());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(refLongtitude());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(refDeviation());
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
