package eu.ldbc.semanticpublishing.validation;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class Validator {
	
	/**
	 * @param result - result from query
	 * @param parameterValues - array of parameter values expected in the result
	 * @param strict - forces full string comparison, if not strict - data types are not checked for
	 * @return 0 it all parameter values have been found in the result
	 * @throws UnsupportedEncodingException 
	 */
	protected int validateEditorial(String result, String validateOperation, boolean resultIsFromAskQuery, int iteration, String[] parameterValues, boolean strict) throws UnsupportedEncodingException {
		int errors = 0;
	
		if (resultIsFromAskQuery) {
			if (!result.toLowerCase().contains(">false<") && !result.toLowerCase().contains(">no<")) {
				errors++;
			}
		} else {		
			if (parameterValues == null) {
				return 1;
			}
			
			for (int i = 0; i < parameterValues.length; i++) {
				String parameterValue = parameterValues[i];
				
				//skip validation of context
				if (parameterValue.contains("/context/")) {
					continue;
				}
				
				parameterValue = transformString(parameterValue, strict, false);
				
				if (!result.contains(parameterValue)) {
					System.out.println(validateOperation + " validation failed on iteration : " + iteration + ", query result is missing value : " + parameterValue);
					errors++;
				}
			}
		}
		return errors;
	}
	
	protected int validateAggregate(String result, String validateOperation, int iteration, List<String> validationList, boolean strict) {
		int errors = 0;
		String value;
		
		for (String v : validationList) {
			int errorsCount = 0;
			
			value = transformString(v, strict, false);
			
			if (!result.contains(value)) {
				errorsCount++;
			}
			
			//give it one more try, in case there are encodings in results which are not URLDecoded
			if (errorsCount > 0) {
				value = transformString(v, strict, true);

				if (!result.contains(value)) {
					errorsCount++;
				} else {
					errorsCount--;
				}
			}
			
			if (errorsCount > 0) {
				System.out.println("\t\t" + validateOperation + " validation failed on query : " + iteration + ", query result is missing value : " + value);
				errors++;
			}
		}
		
		return errors;
	}
	
	private String transformString(String str, boolean strict, boolean forceOptionalEncodings) {
		String result = str;
		if (!strict) {
			if (result.startsWith("<")) {
				result = result.substring(1);
			}
			if (result.endsWith(">")) {
				result = result.substring(0, result.length()-1);
			}				
			if (result.contains("^^")) {
				result = result.substring(0, result.indexOf("^^"));
			}
			
			result = result.replace("\"", "");
		}
		
		result = customURLEncode(result, forceOptionalEncodings);
		
		return result;
	}
	
	private String customURLEncode(String str, boolean forceOptionalEncodings) {
		String result = str;
		if (str.contains("&")) {
			result = str.replace("&", "&amp;");
		}
		if (forceOptionalEncodings) {
			if (str.contains("'")) {
				result = str.replace("'", "&#39;");
			}
		}
		return result;
	}
}
