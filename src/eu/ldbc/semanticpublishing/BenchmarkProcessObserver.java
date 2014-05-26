package eu.ldbc.semanticpublishing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.statistics.Statistics;

/**
 * This class is used to produce a result summary for the benchmark. The thread is scheduled to start at a fixed
 * rate of one second. Results are printed to console and log file.
 */
public class BenchmarkProcessObserver extends Thread {
	private final AtomicLong totalQueryExecutions;
	private final AtomicBoolean benchmarkState;
	private final AtomicBoolean keepAlive;
	private final AtomicBoolean benchmarkResultIsValid;
	private double requiredUpdateRateThresholdOps;
	private double updateRateReachTime;
	private boolean verbose;
	private long seconds;
	private long runPeriodSeconds;
	private long benchmarkByQueryRuns;
	private int requiredUpdateRatePassesCount;
	private int aggregationAgentsCount;
	private int editorialAgentsCount;
	private int initializedCount;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(BenchmarkProcessObserver.class.getName());
	
	public BenchmarkProcessObserver(AtomicLong totalQueryExecutions, AtomicBoolean benchmarkState, AtomicBoolean keepAlive, AtomicBoolean benchmarkResultIsValid, double updateQueryRateFirstReachTimePercent, double requiredUpdateQueriesRateThresholdOps, int editorialAgentsCount, int aggregationAgentsCount, long runPeriodSeconds, long benchmarkByQueryRuns, boolean verbose) {
		this.totalQueryExecutions = totalQueryExecutions;
		this.benchmarkState = benchmarkState;
		this.keepAlive = keepAlive;
		this.benchmarkResultIsValid = benchmarkResultIsValid;
		this.updateRateReachTime = updateQueryRateFirstReachTimePercent;
		this.seconds = 0;
		this.runPeriodSeconds = runPeriodSeconds;
		this.benchmarkByQueryRuns = benchmarkByQueryRuns;
		this.verbose = verbose;
		this.aggregationAgentsCount = aggregationAgentsCount;
		this.editorialAgentsCount = editorialAgentsCount;
		this.requiredUpdateRateThresholdOps = requiredUpdateQueriesRateThresholdOps;
		this.requiredUpdateRatePassesCount = 0;
		this.initializedCount = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * Will be run by a ScheduledThreadPoolExecutor.scheduleAtFixedRate()
	 */
	@Override
	public void run() {
		try {
			while (benchmarkState.get() || keepAlive.get()) {
				seconds++;
				Thread.sleep(1000);
				collectAndShowResults((benchmarkByQueryRuns == 0));
			}
		} catch (InterruptedException ie) {
		}
	}
	
	/**
	 * Displays to console and writes to log file a result summary of the benchmark.
	 * Editorial and Aggregation operations per second.
	 */
	private void collectAndShowResults(boolean secondsOrExecutions) {
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
		if (secondsOrExecutions) {
			sb.append("\nSeconds run : " + seconds);
		} else {
			sb.append("\nQuery executions : " + totalQueryExecutions.get());
		}
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
		
		//keep track of update rate ops
		updateQueriesRateThresoldCheck(averageOperationsPerSecond);
		
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
		
		String message = "";
		
		//in case using requiredUpdateRateThresholdOps option, display a message that benchmark is not 
		if (requiredUpdateRateThresholdOps > 0.0) {
			if (!benchmarkResultIsValid.get()) {
				if ((seconds <= (int)(runPeriodSeconds * updateRateReachTime)) && requiredUpdateRatePassesCount <= 1) {
					message = String.format("Waiting for update operations rate (%.1f) to reach required threshold of %.1f ops in %d second(s)", averageOperationsPerSecond, requiredUpdateRateThresholdOps, ((int)(runPeriodSeconds * updateRateReachTime) - seconds));
					LOGGER.info(message);
					System.out.println(message);
				} else {
					message = String.format("Warning : update operations rate has not reached or has dropped below required threshold of %.1f ops at second : %d, benchmark results are not valid!", requiredUpdateRateThresholdOps, (int)(runPeriodSeconds * updateRateReachTime));
					LOGGER.warn(message);
					System.out.println(message);
					System.exit(0);
				}				
				
				return;
			}
		}
		
		LOGGER.info(sb.toString());
		System.out.println(sb.toString());
	}	
	
	private void updateQueriesRateThresoldCheck(double averageOperationsPerSecond) {
		
		if (requiredUpdateRateThresholdOps <= 0.0 && initializedCount >= 0) {
			//skip setting same values for AtomicBoolean variable : benchmarkResultIsValid, as it is read from other
			if (initializedCount > 0) {
				return;
			}
			//default value for requiredUpdateRateThresholdOps is 0.0 (or if explicitly set in properties file to 0.0), then 
			//disable the UpdateQueriesRateThreshold feature and consider results from benchmark always as valid 
			benchmarkResultIsValid.set(true);
			initializedCount++;
			return;
		}
		
		//the time frame during which update rate should be reached (and kept during the whole benchmark run)
		if (seconds < (runPeriodSeconds * updateRateReachTime)) {
			String message = "";
			//initial reaching of the threshold
			if ((averageOperationsPerSecond >= requiredUpdateRateThresholdOps) && (requiredUpdateRatePassesCount == 0)) {
				message = String.format("Threshold %.1f ops (current update operations rate value : %.1f) has been reached reached at second %d ", requiredUpdateRateThresholdOps, averageOperationsPerSecond, seconds);
				requiredUpdateRatePassesCount++;
				benchmarkResultIsValid.set(true);
				LOGGER.info(message);
				System.out.println(message);
			}
			
			//averageOperationsPerSecond are dropping below the threshold - in which case the benchmark result is considered invalid
			if ((averageOperationsPerSecond < requiredUpdateRateThresholdOps) && (requiredUpdateRatePassesCount == 1)) {
				message = String.format("Warning : Current update operations rate : %.1f ops has dropped below required threshold %.1f at second : %d", averageOperationsPerSecond, requiredUpdateRateThresholdOps, seconds);
				requiredUpdateRatePassesCount++;
				benchmarkResultIsValid.set(false);
				LOGGER.warn(message);
				System.out.println(message);
			}
		//rest of the benchmark run time
		} else {
			//requiredUpdateRatePassedCount should be equal to 1 if threshold was reached during the first time frame, and hasn't dropped after reaching it
			if (requiredUpdateRatePassesCount != 1) {
				benchmarkResultIsValid.set(false);
				return;
			}
			
			if (averageOperationsPerSecond < requiredUpdateRateThresholdOps) {
				benchmarkResultIsValid.set(false);
			}
		}
	}
}
