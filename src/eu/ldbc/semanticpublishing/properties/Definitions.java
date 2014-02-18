package eu.ldbc.semanticpublishing.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import eu.ldbc.semanticpublishing.util.AllocationsUtil;

/**
 * A holder of the benchmark definitions of allocation values.
 * Client is expected to initialize from file definitions.properties first all allocation values. 
 */
public class Definitions {
	public static final String ABOUTS_ALLOCATIONS = "aboutsAllocations";
	public static final String MENTIONS_ALLOCATIONS = "mentionsAllocations";
	public static final String ENTITY_POPULARITY = "entityPopularity";
	public static final String USE_POPULAR_ENTITIES = "usePopularEntities";
	public static final String CREATIVE_WORK_TYPES_ALLOCATION = "creativeWorkTypesAllocation";
	public static final String ABOUT_AND_MENTIONS_ALLOCATION = "aboutAndMentionsAllocation";
	public static final String EDITORIAL_OPERATIONS_ALLOCATION = "editorialOperationsAllocation";
	public static final String AGGREGATION_OPERATIONS_ALLOCATION = "aggregationOperationsAllocation";
	public static final String EXPONENTIAL_DECAY_UPPER_LIMIT_OF_CWS = "exponentialDecayUpperLimitOfCWs";
	public static final String EXPONENTIAL_DECAY_RATE = "exponentialDecayRate";
	public static final String EXPONENTIAL_DECAY_THRESHOLD_PERCENT = "exponentialDecayThresholdPercent";
	public static final String MAJOR_EVENTS_PER_YEAR = "majorEventsPerYear";
	public static final String MINOR_EVENT_PER_YEAR = "minorEventsPerYear";
	public static final String YEAR_SEED = "seedYear";
	
	//About tags in Creative Works
	public static AllocationsUtil aboutsAllocations;
	
	//Mentions tags in Creative Works, e.g. 3.81% - 1 mentions tag, 0.93% - 2 mentions tags ... etc. 94.77% - CWs with no mentions tags
	public static AllocationsUtil mentionsAllocations;
	
	//Determines the popularity of an entity, e.g. 5% - popular, 95% - regular
	public static AllocationsUtil entityPopularity;	
	
	//Determines allocation of popular to regular tagging, e.g. 30% of cases when needed popular entities will be used
	public static AllocationsUtil usePopularEntities;
	
	//Determines allocation of Creative Work Types, e.g. 45% - BlogPost, 35% - NewsItem, 20% - Program
	public static AllocationsUtil creativeWorkTypesAllocation;
	
	//Determines the aggregation type, aggregate on about or on mentions property
	public static AllocationsUtil aboutAndMentionsAllocation;
	
	//Determines the editorial operations distribution, e.g. insert - 80%, update - 10%, delete - 10%
	public static AllocationsUtil editorialOperationsAllocation;	
	
	//Determines the aggregation operations distribution, e.g. query1 - 80%, query2 - 20%
	public static AllocationsUtil aggregationOperationsAllocation;
	
	private static final Properties definitionsProperties = new Properties();
	
	private boolean verbose = false;
	
	/**
	 * Load the configuration from the given file (java properties format).
	 * @param filename A readable file on the file system.
	 * @throws IOException
	 */
	public void loadFromFile(String filename, boolean verbose) throws IOException {
		
		InputStream input = new FileInputStream(filename);
		try {
			definitionsProperties.load(input);
		}
		finally {
			input.close();
		}
		this.verbose = verbose;
		initialize();
	}
	
	/**
	 * Read a definition parameter's value as a string
	 * @param key
	 * @return
	 */
	private String getString( String key) {
		String value = definitionsProperties.getProperty(key);
		
		if(value == null) {
			throw new IllegalStateException( "Missing definitions parameter: " + key);
		}
		return value;
	}
	
	/**
	 * Read a configuration parameter's value as an int
	 * @param key
	 * @return
	 */
	public int getInt(String key) {
		String value = getString(key);
		
		try {
			return Integer.parseInt(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for integer configuration parameter: " + key);
		}
	}

	/**
	 * Read a configuration parameter's value as a long
	 * @param key
	 * @return
	 */
	public long getLong(String key) {
		String value = getString(key);
		
		try {
			return Long.parseLong(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for long integer configuration parameter: " + key);
		}
	}
	
	/**
	 * Read a configuration parameter's value as a Double
	 * @param key
	 * @return
	 */
	public double getDouble(String key) {
		String value = getString(key);
		
		try {
			return Double.parseDouble(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for long integer configuration parameter: " + key);
		}
	}	
	
	private void initialize() {
		if (verbose) {
			System.out.println("Initializing allocations...");
		}
		
		initializeAllocation(ABOUTS_ALLOCATIONS);
		initializeAllocation(MENTIONS_ALLOCATIONS);
		initializeAllocation(ENTITY_POPULARITY);
		initializeAllocation(USE_POPULAR_ENTITIES);
		initializeAllocation(CREATIVE_WORK_TYPES_ALLOCATION);
		initializeAllocation(ABOUT_AND_MENTIONS_ALLOCATION);
		initializeAllocation(EDITORIAL_OPERATIONS_ALLOCATION);
		initializeAllocation(AGGREGATION_OPERATIONS_ALLOCATION);
	}
	
	/**
	 * Initialize allocations depending on allocationProperty name
	 */
	private void initializeAllocation(String allocationPorpertyName) {
		String allocations = getString(allocationPorpertyName);
		String[] allocationsAsStrings = allocations.split(",");
		double[] allocationsAsDoubles = new double[allocationsAsStrings.length];
		
		for (int i = 0; i < allocationsAsDoubles.length; i++) {
			allocationsAsDoubles[i] = Double.parseDouble(allocationsAsStrings[i]);
		}
		
		if (allocationPorpertyName.equals(ABOUTS_ALLOCATIONS)) {
			aboutsAllocations = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(MENTIONS_ALLOCATIONS)) {
			mentionsAllocations = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(ENTITY_POPULARITY)) {
			entityPopularity = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(USE_POPULAR_ENTITIES)) {
			usePopularEntities = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(CREATIVE_WORK_TYPES_ALLOCATION)) {
			creativeWorkTypesAllocation = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(ABOUT_AND_MENTIONS_ALLOCATION)) {
			aboutAndMentionsAllocation = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(EDITORIAL_OPERATIONS_ALLOCATION)) {
			editorialOperationsAllocation = new AllocationsUtil(allocationsAsDoubles);
		} else if (allocationPorpertyName.equals(AGGREGATION_OPERATIONS_ALLOCATION)) {
			aggregationOperationsAllocation = new AllocationsUtil(allocationsAsDoubles);
		}
		
		if (verbose) {
			System.out.println(String.format("\t%-33s : {%s}", allocationPorpertyName, allocations));
		}
	}
}
