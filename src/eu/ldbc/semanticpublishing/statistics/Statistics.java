package eu.ldbc.semanticpublishing.statistics;

public class Statistics {
	private static final String INSERT_QUERIES_STATISTICS = "INSERT";
	private static final String UPDATE_QUERIES_STATISTICS = "UPDATE";
	private static final String DELETE_QUERIES_STATISTICS = "DELETE";
	private static final String AGGREGATE_QUERIES_STATISTICS = "AGGREGATE";
	
	public static final int AGGREGATE_QUERIES_COUNT = 26;
	
	public static final String AGGREGATE_QUERY_NAME = "query";
	public static final QueryStatistics[] aggregateQueriesArray;
	
	static {
		aggregateQueriesArray = new QueryStatistics[AGGREGATE_QUERIES_COUNT];
		
		for (int i = 0; i < AGGREGATE_QUERIES_COUNT; i++) {
			aggregateQueriesArray[i] = new QueryStatistics(AGGREGATE_QUERIES_STATISTICS + "_" + (i + 1));
		}
	}
	
	//section for keeping statistics for each executed query type
	public static final QueryStatistics insertCreativeWorksQueryStatistics = new QueryStatistics(INSERT_QUERIES_STATISTICS);
	public static final QueryStatistics updateCreativeWorksQueryStatistics = new QueryStatistics(UPDATE_QUERIES_STATISTICS);
	public static final QueryStatistics deleteCreativeWorksQueryStatistics = new QueryStatistics(DELETE_QUERIES_STATISTICS);
	public static final QueryStatistics totalAggregateQueryStatistics = new QueryStatistics(AGGREGATE_QUERIES_STATISTICS);
}
