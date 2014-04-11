package eu.ldbc.semanticpublishing.generators.querygenerator;

import java.io.BufferedWriter;
import java.io.IOException;

public interface QueryParametersGenerator {
	public static final String PARAMS_DELIMITER = ";";
	public static final String LIST_DELIMITER = "|L|";
	public void generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException;
}
