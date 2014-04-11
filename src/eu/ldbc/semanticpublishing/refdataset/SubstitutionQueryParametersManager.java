package eu.ldbc.semanticpublishing.refdataset;

import java.io.File;
import java.io.IOException;

import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.util.StringUtil;

/**
 * A class which will manage initialization and consumption of generated
 * substitution query parameters
 */
public class SubstitutionQueryParametersManager {
	public static final SubstitutionQueryParameters[] substitutionParametersQueues;

	private static final String QUERY_NAME = "query";
	private static final String SUBST_PARAMETERS_FILE_SUFFIX = "SubstParameters.txt";

	static {
		substitutionParametersQueues = new SubstitutionQueryParameters[Statistics.AGGREGATE_QUERIES_COUNT];
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			substitutionParametersQueues[i] = new SubstitutionQueryParameters(String.format("%s%d%s", QUERY_NAME, (i + 1),	SUBST_PARAMETERS_FILE_SUFFIX));
		}
	}

	public void intiSubstitutionParameters(String location) throws IOException,	InterruptedException {
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			substitutionParametersQueues[i].initFromFile(buildSubstParametersFilePath(location,	substitutionParametersQueues[i].getQueryName()));
		}
	}

	private String buildSubstParametersFilePath(String location, String queryName) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtil.normalizePath(location));
		sb.append(File.separator);
		sb.append(queryName);
		return sb.toString();
	}

	/**
	 * @param queryIndex
	 *            - Notice - queryIndex is zero based, while substitution query
	 *            parameters are NOT
	 */
	public SubstitutionQueryParameters getSubstitutionParametersFor(
			int queryIndex) {
		return substitutionParametersQueues[queryIndex];
	}
}