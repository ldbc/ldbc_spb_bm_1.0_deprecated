package eu.ldbc.semanticpublishing.templates.editorial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplateCompiler, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/editorial/insert.txt
 */
public class InsertTemplate extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "insert.txt";
	
	private CWType cwType = CWType.BLOG_POST;
	private String cwTypeString = "cwork:BlogPost";
	private String contextURI;
	private int aboutsCount = 0;
	private int mentionsCount = 0;	
	private Entity cwEntity;
	private boolean initialAboutUriUsed = false;
	private boolean geonamesLocationUsed = false;
	
	private final RandomUtil ru;
	
	private static enum CWType {
		BLOG_POST, NEWS_ITEM, PROGRAMME
	}	
	
	public InsertTemplate(String contextURI, RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates);
		this.contextURI = contextURI;
		this.ru = ru;
		this.aboutsCount = Definitions.aboutsAllocations.getAllocation();
		this.mentionsCount = Definitions.mentionsAllocations.getAllocation();
		initializeCreativeWorkEntity(contextURI);
	}
	
	/**
	 * Creates an Entity with existing/new Creative Work URI and the label of a random (popular or not) existing entity.
	 * Label will be used in the title of that Creative Work
	 * @param updateCwUri - if empty, a new URI is generated
	 * @return the CreativeWork entity
	 */
	private void initializeCreativeWorkEntity(String updateCwUri) {
		Entity e;
		String cwURInew;
		try {			
			if (!updateCwUri.isEmpty()) {
				cwURInew = updateCwUri;
			} else {
				cwURInew = ru.numberURI("things", DataManager.creativeWorksNexId.incrementAndGet(), true, true);				
			}
			
			this.contextURI = cwURInew.replace("/things/", "/context/");
			
			switch (Definitions.creativeWorkTypesAllocation.getAllocation()) {
				case 0 :
					this.cwType = CWType.BLOG_POST;
					this.cwTypeString = "cwork:BlogPost";
					break;
				case 1 :
					this.cwType = CWType.NEWS_ITEM;
					this.cwTypeString = "cwork:NewsItem";
					break;
				case 2 :
					this.cwType = CWType.PROGRAMME;
					this.cwTypeString = "cwork:Programme";
					break;					
			}
			
			boolean usePopularEntity = Definitions.usePopularEntities.getAllocation() == 0;
			
			if (usePopularEntity) {
				e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			} else {
				e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
			}
			
			this.cwEntity = new Entity(cwURInew, e.getLabel(), e.getURI(), e.getCategory());
		} catch (IllegalArgumentException iae) {
			if (DataManager.popularEntitiesList.size() + DataManager.regularEntitiesList.size() == 0) {
				System.err.println("No reference data found in repository, initialize reposotory with ontologies and reference data first!");
			}
			throw new IllegalArgumentException(iae);
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwGraphUri}}}
	 */		
	public String cwGraphUri() {
		return this.contextURI;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwUri}}}
	 */		
	public String cwUri() {
		return this.contextURI.replace("/context/", "/things/");
	}

	/**
	 * A method for replacing mustache template : {{{cwType}}}
	 */		
	public String cwType() {
		return this.cwTypeString;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwTitle}}}
	 */		
	public String cwTitle() {
		return ru.sentenceFromDictionaryWords(this.cwEntity.getLabel(), 10, true, true);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwShortTitle}}}
	 */		
	public String cwShortTitle() {
		return ru.sentenceFromDictionaryWords("", 10, true, true);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwCategory}}}
	 */		
	public String cwCategory() {
		return ru.stringURI("category", cwEntity.getCategory(), true, false);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDescription}}}
	 */		
	public String cwDescription() {
		return ru.sentenceFromDictionaryWords("", ru.nextInt(8, 26), true, true);
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwAboutsList}}} {{{/cwAboutsList}}}
	 */	
	public List<Object> cwAboutsList() {
		  List<Object> abouts = new ArrayList<Object>();

		  //using aboutsCount + 1, because Definitions.aboutsAllocations.getAllocation() returning 0 is still a valid allocation
		  for (int i = 0; i < aboutsCount + 1; i++) {
			  if (!initialAboutUriUsed) {
				  initialAboutUriUsed = true;
				  abouts.add(new AboutUri(this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT)));
			  } else {
				  abouts.add(new AboutUri(DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI()));
			  }
		  }
		  
		  return abouts;
	}
	
	/**
	 * A class for replacing mustache template : {{{CwAboutUri}}}, part of the cwAboutsList
	 */		
	static class AboutUri {
		String cwAboutUri;
		public AboutUri(String aboutUri) {
			this.cwAboutUri = aboutUri;
		}
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwAboutsList}}} {{{/cwAboutsList}}}
	 */		
	public List<Object> cwMentionsList() {
		List<Object> mentions = new ArrayList<Object>();
		
		 //using mentionsCount + 1, because Definitions.mentionsAllocations.getAllocation() returning 0 is still a valid allocation		
		for (int i = 0; i < mentionsCount + 1; i++) {
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
				mentions.add(new MentionsUri(this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT)));
			} else {
				if (!geonamesLocationUsed) {
					geonamesLocationUsed = true;
					mentions.add(new MentionsUri(DataManager.geonamesIdsList.get(ru.nextInt(DataManager.geonamesIdsList.size()))));
				} else {
					mentions.add(new MentionsUri(DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI()));
				}
			}
		}
		
		return mentions;
	}
	
	/**
	 * A class for replacing mustache template : {{{CwMentionsUri}}}, part of the cwMentionsList
	 */
	static class MentionsUri {
		String cwMentionsUri;
		public MentionsUri(String mentionsUri) {
			this.cwMentionsUri = mentionsUri;
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudienceType}}}
	 */		
	public String cwAudienceType() {
		switch (cwType) {		
		case BLOG_POST :
			return "cwork:InternationalAudience";
		case NEWS_ITEM :
			return "cwork:NationalAudience";
		case PROGRAMME :
			return "cwork:InternationalAudience";
		default :
			return "cwork:InternationalAudience";
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwLiveCoverage}}}
	 */		
	public String cwLiveCoverage() {
		switch (cwType) {		
		case BLOG_POST :
			return ru.createBoolean(false);
		case NEWS_ITEM :
			return ru.createBoolean(false);
		case PROGRAMME :
			return ru.createBoolean(true);
		default :
			return ru.createBoolean(true);
		}
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwPrimaryFormatList}}} {{{/cwPrimaryFormatList}}}
	 */		
	public List<Object> cwPrimaryFormatList() {
		List<Object> format = new ArrayList<Object>();
		
		switch (cwType) {		
		case BLOG_POST :
			format.add(new PrimaryFormat("cwork:TextualFormat"));
			if (ru.nextBoolean()) {
				format.add(new PrimaryFormat("cwork:InteractiveFormat"));
			}
		case NEWS_ITEM :
			format.add(new PrimaryFormat("cwork:TextualFormat"));
			format.add(new PrimaryFormat("cwork:InteractiveFormat"));
		case PROGRAMME :
			if (ru.nextBoolean()) {
				format.add(new PrimaryFormat("cwork:VideoFormat"));
			} else {
				format.add(new PrimaryFormat("cwork:AudioFormat"));
			}
		}
		return format;
	}
	
	/**
	 * A class for replacing mustache template : {{{cwPrimaryFormat}}}, part of the cwPrimaryFormatList
	 */	
	static class PrimaryFormat {
		String cwPrimaryFormat;
		public PrimaryFormat(String primaryFormat) {
			this.cwPrimaryFormat = primaryFormat;
		}
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDateCreated}}}
	 */	
	public String cwDateCreated() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1 * ru.nextInt(12));
		calendar.add(Calendar.DATE, -1 * ru.nextInt(31));
		calendar.add(Calendar.HOUR, -1 * ru.nextInt(24));
		return ru.dateTimeString(calendar.getTime());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwDateModified}}}
	 */		
	public String cwDateModified() {
		return ru.currentDateTimeString();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwThumbnailUri}}}
	 */	
	public String cwThumbnailUri() {
		return ru.randomURI("thumbnail", true, false);
	}
	
	/**
	 * A method for replacing mustache template list : {{{#cwPrimaryContentList}}} {{{/cwPrimaryContentList}}}
	 */		
	public List<Object> cwPrimaryContentList() {
		List<Object> primaryContent = new ArrayList<Object>();
		
		for (int i = 0; i < ru.nextInt(1, 4); i++) {
			primaryContent.add(new PrimaryContentUri(ru.randomURI("things", true, true), ru.nextBoolean() ? "bbc:HighWeb" : "bbc:Mobile"));
		}
		
		return primaryContent;
	}
	
	/**
	 * A class for replacing mustache template : {{{cwPrimaryContentUri}}} and {{{cwWebDocumentType}}}, part of the cwPrimaryFormatList
	 */	
	static class PrimaryContentUri {
		String cwPrimaryContentUri;
		String cwWebDocumentType;
		public PrimaryContentUri(String primaryContentUri, String webDocumentType) {
			this.cwPrimaryContentUri = primaryContentUri;
			this.cwWebDocumentType = webDocumentType;
		}
	}
	
	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.INSERT;
	}
}
