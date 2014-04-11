package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.generators.querygenerator.QueryParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query18.txt
 * A time range drill-down query template
 */
public class Query18Template extends MustacheTemplate implements QueryParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query18.txt";
	
	protected final RandomUtil ru;
	private int year;
	private int month;
	private int day;
	private int maxDayOfMonth;
	private int hour;
	private int minute;
	private int deviation;
	private int iteration;
	private int seedYear;

	public Query18Template(RandomUtil ru, HashMap<String, String> queryTemplates, int seedYear, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		this.seedYear = seedYear;
		preInitialize();
	}
	
	protected void preInitialize() {
		Calendar calendar = Calendar.getInstance();
		//Initializing year with a value that is certain to be used. see RandomUtil.YEARS_OFFSET
		year = this.seedYear;
		month = ru.nextInt(1, 12);
		calendar.set(year, month - 1, 1);		
		day = ru.nextInt(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		hour = ru.nextInt(0, 23);
		minute = ru.nextInt(0, 59);
		deviation = 0;
		iteration = 0;
		parameterIndex = 0;
	}	
	
	public void initialize(String dateTimeString, int deviation, String[] substitutionParameters) {
		//example of dateTime literal : 2012-08-22T18:22:38.240+03:00
		if (dateTimeString.indexOf("T") > 0) {
			String dateString = dateTimeString.substring(0, dateTimeString.indexOf("T"));
			String[] tokens = dateString.split("-");
			if (tokens.length == 3) {
//				int year = Integer.parseInt(dateTokens[0]);
				//TODO : if line below is uncommented, maxDayOfMonth should be updated too								
//				this.month = Integer.parseInt(dateTokens[1]);
				this.day = Integer.parseInt(tokens[2]);
			}
			
			String timeString;
			if (dateTimeString.indexOf(".") > 0) {
				timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 1, dateTimeString.indexOf("."));
			} else {
				timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 1, dateTimeString.indexOf("T") + 9);
			}	
			tokens = timeString.split(":");			
			if (tokens.length == 3) {
				this.hour = Integer.parseInt(tokens[0]);
				this.minute = Integer.parseInt(tokens[1]);
//				this.seconds = Integer.parseInt(tokens[2]);
			}
		}
		
		this.deviation = deviation;
		this.substitutionParameters = substitutionParameters;
		
		iteration++;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwType}}}
	 */	
	public String cwType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		String cwType = "cwork:NewsItem";
		int cwTypeAllocation = ru.nextInt(3);
		switch (cwTypeAllocation) {
		case 0:
			cwType = "cwork:NewsItem";
			break;
		case 1:
			cwType = "cwork:BlogPost";
			break;
		case 2:
			cwType = "cwork:Programme";
			break;
		}
		return cwType;
	}

	/**
	 * A method for replacing mustache template : {{{cwFilterDateModifiedCondition}}}
	 * with a FILTER constraint evaluating time range conditions
	 */		
	public String cwFilterDateModifiedCondition() {	
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		if (iteration % 4 == 0) {
			//first iteration starts with a filter constraint for the whole month
			if (iteration > 0) {
				//5th++ iterations will start with a new randomly selected month
				month = ru.nextInt(1, 12);
			}
			return generateFilterDateString("dateModif", year, month, -1, -1, -1, -1, -1, -1);
		} else if (iteration % 4 == 1) {
			//further limiting to days range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, -1, -1, -1, -1);
		} else if (iteration % 4 == 2) {
			//further limiting to hours range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, hour, (hour + deviation) < 24 ? (hour + deviation) : hour, -1, -1);
		} else if (iteration % 4 == 3) {
			//further limiting to minutes range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, hour, (hour + deviation) < 24 ? (hour + deviation) : hour, minute, (minute + deviation) < 60 ? (minute + deviation) : minute);
		}
		return generateFilterDateString("dateModif", year, month, -1, -1, -1, -1, -1, -1);
	}

	/**
	 * Input parameters year and month are compulsory
	 * @param variableName - name of the variable used to hold the date in the SPARQL query
	 * @param year - the year used in the constraint
	 * @param month - the month used in the constraint, IF < 0 it is not used in the constraint
	 * @param day1 - start of the period, if < 0 it is not used
	 * @param day2 - end period, if < 0 it is not used
	 * @param hour1 - start hour, if < 0 it is not used
	 * @param hour2 - end hour, if < 0 it is not used
	 * @param minute1 - start minute, if < 0 it is not used
	 * @param minute2 - end minute, if < 0 it is not used
	 * @return SPARQL formatted string with FILTER constraint 
	 */
	private String generateFilterDateString(String variableName,int year, int month, int day1, int day2, int hour1, int hour2, int minute1, int minute2) {
		StringBuilder sb = new StringBuilder();
		StringBuilder sbStartRange = new StringBuilder();
		StringBuilder sbEndRange = new StringBuilder();
		
		Calendar calendar = Calendar.getInstance();
		
		calendar.set(year, month - 1, 1);
		
		sbStartRange.append("\"");
		sbEndRange.append("\"");
		
		sbStartRange.append(year);
		sbEndRange.append(year);
		
		sbStartRange.append("-");
		sbEndRange.append("-");
		
		if (month > 0) {
			sbStartRange.append(String.format("%02d", month));
			sbEndRange.append(String.format("%02d", month));
		} else {
			sbStartRange.append("01");
			sbEndRange.append("12");
		}
		sbStartRange.append("-");
		sbEndRange.append("-");
				
		if (day1 > 0) { 
			sbStartRange.append(String.format("%02d", day1));			
		} else {
			sbStartRange.append("01");
		}

		if (day2 > 0) {
			sbEndRange.append(String.format("%02d", day2));
		} else {
			sbEndRange.append(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		}	
		
		sbStartRange.append("T");
		sbEndRange.append("T");
		
		if (hour1 > 0) {
			sbStartRange.append(String.format("%02d", hour1));
		} else {
			sbStartRange.append("00");
		}
		sbStartRange.append(":");
		
		if (hour2 > 0) {
			sbEndRange.append(String.format("%02d", hour2));
		} else {
			sbEndRange.append("23");
		}
		sbEndRange.append(":");
		
		if (minute1 > 0) {
			sbStartRange.append(String.format("%02d", minute1));
		} else {
			sbStartRange.append("00");
		}
		sbStartRange.append(":");
		
		if (minute2 > 0) {
			sbEndRange.append(String.format("%02d", minute2));
		} else {
			sbEndRange.append("59");
		}
		sbEndRange.append(":");

		sbStartRange.append("00.000");
		sbEndRange.append("59.999");
		
		sbStartRange.append("\"");
		sbEndRange.append("\"");
		
		sbStartRange.append("^^<http://www.w3.org/2001/XMLSchema#dateTime>");
		sbEndRange.append("^^<http://www.w3.org/2001/XMLSchema#dateTime>");
		
		sb.append("FILTER(");
		sb.append("?");
		sb.append(variableName);
		sb.append(" >= ");
		sb.append(sbStartRange);
		sb.append(" && ");
		sb.append("?");
		sb.append(variableName);
		sb.append(" < ");
		sb.append(sbEndRange);
		sb.append(") . ");		

		return sb.toString();
	}	
	
	@Override
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwType());
			sb.append(QueryParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwFilterDateModifiedCondition());
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
