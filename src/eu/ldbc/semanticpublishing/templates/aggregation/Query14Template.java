package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query14.txt
 */
public class Query14Template  extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query14.txt";

	private final RandomUtil ru;
	
	private int creativeWorkType;
	
	private static final String[] categoryTypes = {	"cwork:TextualFormat", 
													"cwork:ImageFormat", 
													"cwork:PictureGalleryFormat", 
													"cwork:AudioFormat", 
													"cwork:VideoFormat", 
													"cwork:InteractiveFormat"
	};	
	
	public Query14Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;	
		preInitialize();
	}
	
	private void preInitialize() {
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */
	public String cwAudienceType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		

		switch (creativeWorkType) {
		//cwork:BlogPost
		case 0 :
			return "cwork:InternationalAudience";
		//cwork:NewsItem
		case 1 :
			return "cwork:NationalAudience";
		//cwork:Programme
		case 2 :
			return "cwork:InternationalAudience";
		}
		return "cwork:InternationalAudience";		
	}

	/**
	 * A method for replacing mustache template : {{{cwWebDocumentType}}}
	 */
	public String cwWebDocumentType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.nextBoolean() ? "bbc:HighWeb" : "bbc:Mobile";
	}
	
	/**
	 * A method for replacing mustache template : {{{cwPrimaryFormat}}}
	 */	
	public String cwPrimaryFormat() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return categoryTypes[ru.nextInt(categoryTypes.length)];
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwAudienceType());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwAudienceType());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwWebDocumentType());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwPrimaryFormat());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwPrimaryFormat());
			sb.append("\n");
			bw.write(sb.toString());
		}	
		return null;
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
