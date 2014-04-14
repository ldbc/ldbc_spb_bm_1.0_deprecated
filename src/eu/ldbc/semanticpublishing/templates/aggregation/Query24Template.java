package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query24.txt
 * A time-line of relatedness query
 */
public class Query24Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query24.txt";
	private static final int TIME_PERIOD_MONTHS = 9;
	
	protected final RandomUtil ru;
	private int correlationPosition;
	
//	private boolean useCorrelatedEntities = false;
	
	public Query24Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;		
		preInitialize();
	}
	
	private void preInitialize() {
//		this.useCorrelatedEntities = ru.nextBoolean();
		if (DataManager.correlatedEntitiesList.size() > 0 /*&& useCorrelatedEntities*/) {
			//correlatedEntitiesList contains URIs of correlated entities in the sequence : entityA1, entityB1, entityC1, entityA2, entityB2, entityC2...etc.
			this.correlationPosition = ru.nextInt(DataManager.correlatedEntitiesList.size() / 3);
		}		
	}
	
	/**
	 * A method for replacing mustache template : {{{entityA}}}
	 */
	public String entityA() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		if (DataManager.correlatedEntitiesList.size() > 0 /*&& useCorrelatedEntities*/) {
			return DataManager.correlatedEntitiesList.get(correlationPosition * 3);
		} else {
			return DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size())).getURI();
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{entityB}}}
	 */
	public String entityB() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		if (DataManager.correlatedEntitiesList.size() > 0 /*&& useCorrelatedEntities*/) {
			return DataManager.correlatedEntitiesList.get(correlationPosition * 3 + 1);
		} else {
			return DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI();
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{timeFilter}}}
	 */
	public String timeFilter() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		Date date = ru.randomDateTime(-1, 12);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		String startDate = ru.dateTimeString(calendar.getTime());
		
		calendar.add(Calendar.MONTH, TIME_PERIOD_MONTHS);
		
		String endDate = ru.dateTimeString(calendar.getTime());
		
		return String.format("FILTER (?dateCreated >= %s && ?dateCreated < %s) . ", startDate, endDate);
	}
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(entityA());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(entityB());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(timeFilter());
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
