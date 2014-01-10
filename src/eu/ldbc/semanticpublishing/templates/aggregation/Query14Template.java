package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query14.txt
 */
public class Query14Template  extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query14.txt";

	private final int creativeWorkType;
	private final RandomUtil ru;
	
	private static final String[] categoryTypes = {	"cwork:TextualFormat", 
													"cwork:ImageFormat", 
													"cwork:PictureGalleryFormat", 
													"cwork:AudioFormat", 
													"cwork:VideoFormat", 
													"cwork:InteractiveFormat"
	};	
	
	public Query14Template(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */
	public String cwAudienceType() {
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
		return ru.nextBoolean() ? "bbc:HighWeb" : "bbc:Mobile";
	}
	
	/**
	 * A method for replacing mustache template : {{{cwPrimaryFormat}}}
	 */	
	public String cwPrimaryFormat() {
		return categoryTypes[ru.nextInt(categoryTypes.length)];
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
