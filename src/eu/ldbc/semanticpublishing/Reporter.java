package eu.ldbc.semanticpublishing;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.statistics.Statistics;

/**
 * This class is used to produce a result summary for the benchmark. The thread is scheduled to start at a fixed
 * rate of one second. Results are printed to console and log file.
 */
public class Reporter implements Runnable {
	private final AtomicBoolean benchmarkState;
	private boolean verbose;
	private long seconds;
	private int aggregationAgentsCount;
	private int editorialAgentsCount;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(Reporter.class.getName());
	
	public Reporter(AtomicBoolean benchmarkState, int editorialAgentsCount, int aggregationAgentsCount, long runPeriodSeconds, boolean verbose) {
		this.benchmarkState = benchmarkState;
		this.seconds = 0;
		this.verbose = verbose;
		this.aggregationAgentsCount = aggregationAgentsCount;
		this.editorialAgentsCount = editorialAgentsCount;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * Will be run by a ScheduledThreadPoolExecutor.scheduleAtFixedRate()
	 */
	@Override
	public void run() {
		if (benchmarkState.get()) {
			seconds++;
			displayResultsSummary();
		} 
	}
	
	/**
	 * Displays to console and writes to log file a result summary of the benchmark.
	 * Editorial and Aggregation operations per second.
	 */
	private void displayResultsSummary() {
		StringBuilder sb = new StringBuilder();
		
		long insertOpsCount = Statistics.insertCreativeWorksQueryStatistics.getRunsCount();
		long updateOpsCount = Statistics.updateCreativeWorksQueryStatistics.getRunsCount();
		long deleteOpsCount = Statistics.deleteCreativeWorksQueryStatistics.getRunsCount();
		long totalAggregateOpsCount = Statistics.totalAggregateQueryStatistics.getRunsCount();
		
		long failedInsertOpsCount = Statistics.insertCreativeWorksQueryStatistics.getFailuresCount();
		long failedUpdateOpsCount = Statistics.updateCreativeWorksQueryStatistics.getFailuresCount();
		long failedDeleteOpsCount = Statistics.deleteCreativeWorksQueryStatistics.getFailuresCount();
		long failedTotalAggregateOpsCount = Statistics.totalAggregateQueryStatistics.getFailuresCount();
		
		sb.append("\n");
		sb.append("\nSeconds run : " + seconds);
		sb.append("\n");
		sb.append("\tEditorial:\n");
		sb.append(String.format("\t\t%s agents\n\n", editorialAgentsCount));
		if (verbose) {
			
			sb.append(String.format("\t\t%-5d inserts (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", insertOpsCount ,Statistics.insertCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.insertCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.insertCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append(String.format("\t\t%-5d updates (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", updateOpsCount ,Statistics.updateCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.updateCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.updateCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append(String.format("\t\t%-5d deletes (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", deleteOpsCount ,Statistics.deleteCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.deleteCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.deleteCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append("\n");
			sb.append(String.format("\t\t%d operations (%d CW Inserts (%d failed), %d CW Updates (%d failed), %d CW Deletions (%d failed))\n", ( insertOpsCount + updateOpsCount + deleteOpsCount ),
																	  																			 insertOpsCount, failedInsertOpsCount,
																	  																			 updateOpsCount, failedUpdateOpsCount,
																	  																			 deleteOpsCount, failedDeleteOpsCount) );
		} else {
			sb.append(String.format("\t\t%d operations (%d CW Inserts, %d CW Updates, %d CW Deletions)\n", ( insertOpsCount + updateOpsCount + deleteOpsCount ),
																											 insertOpsCount, 
																											 updateOpsCount,
																											 deleteOpsCount) );
		}

		double averageOperationsPerSecond = (double)(insertOpsCount + updateOpsCount + deleteOpsCount) / (double)seconds;
		sb.append(String.format("\t\t%.4f average operations per second\n", averageOperationsPerSecond));

		sb.append("\n");
		sb.append("\tAggregation:\n");
		sb.append(String.format("\t\t%s agents\n\n", aggregationAgentsCount));
		if (verbose) {
			for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
				sb.append(String.format("\t\t%-5d Q%-2d  queries (avg : %-7d ms, min : %-7d ms, max : %-7d ms, %d failed)\n", Statistics.aggregateQueriesArray[i].getRunsCount(), 
																											   				  (i + 1),
																											   				  Statistics.aggregateQueriesArray[i].getAvgExecutionTimeMs(),
																											   				  Statistics.aggregateQueriesArray[i].getMinExecutionTimeMs(), 
																											   				  Statistics.aggregateQueriesArray[i].getMaxExecutionTimeMs(), 
																											   				  Statistics.aggregateQueriesArray[i].getFailuresCount()));
			}
			sb.append(String.format("\n\t\t%d total retrieval queries (%d failed)\n", totalAggregateOpsCount, failedTotalAggregateOpsCount));
		} else {
			for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
				sb.append(String.format("\t\t%-5d Q%-2d  queries\n", Statistics.aggregateQueriesArray[i].getRunsCount(), (i + 1)));
			}
			
			sb.append(String.format("\n\t\t%d total retrieval queries\n", totalAggregateOpsCount));
		}
		
		double averageQueriesPerSecond = (double)totalAggregateOpsCount / (double)seconds;
		sb.append(String.format("\t\t%.4f average queries per second\n", averageQueriesPerSecond));	
		
		LOGGER.info(sb.toString());
		System.out.println(sb.toString());
	}	
}
