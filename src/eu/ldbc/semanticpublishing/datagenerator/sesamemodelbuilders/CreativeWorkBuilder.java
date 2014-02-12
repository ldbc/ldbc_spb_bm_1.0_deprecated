package eu.ldbc.semanticpublishing.datagenerator.sesamemodelbuilders;

import java.util.Calendar;
import java.util.Date;

import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;

import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.RandomUtil;

public class CreativeWorkBuilder implements SesameBuilder {

	private Date presetDate;
	private CWType cwType = CWType.BLOG_POST;
	private String cwTypeString = "cwork:BlogPost";
	private String contextURI;
	private String presetAboutTagUri;
	private int aboutsCount = 0;
	private int mentionsCount = 0;	
	private Entity cwEntity;
	private boolean usePresetDate = false;
	private boolean usePresetAboutTagUri = false;
	
	private final RandomUtil ru;
	
	private static final String rdfTypeNamespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private static final String cworkNamespace = "http://www.bbc.co.uk/ontologies/creativework/";
	private static final String bbcNamespace = "http://www.bbc.co.uk/ontologies/bbc/";
	
	private static enum CWType {
		BLOG_POST, NEWS_ITEM, PROGRAMME
	}
	
	public CreativeWorkBuilder(String contextURI, RandomUtil ru) {
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
	
	public void setPresetDate(Date date) {
		this.presetDate = date;
	}
	
	public void setUsePresetDate(boolean usePresetDate) {
		this.usePresetDate = true;
	}
	
	public void setPresetAboutTag(String aboutTagUri) {
		this.presetAboutTagUri = aboutTagUri;
	}
	
	public void setUsePresetAboutTag(boolean usePresetAboutTagUri) {
		this.usePresetAboutTagUri = usePresetAboutTagUri;
	}
	
	/**
	 * Builds a Sesame Model of the Insert query template using values from templateParameterValues array.
	 * Which gets initialized with values during construction of the object.
	 */
	@Override
	public Model buildSesameModel() {
		Model model = new LinkedHashModel();
		
		String adaptedContextUri = contextURI.replace("<", "").replace(">", "");
		URI context = sesameValueFactory.createURI(adaptedContextUri);
		
		//Set Creative Work Type
		URI subject = sesameValueFactory.createURI(adaptedContextUri.replace("/context/", "/things/"));
		URI predicate = sesameValueFactory.createURI(rdfTypeNamespace);
		
		String s = cwTypeString;
		s = s.replace("cwork:", cworkNamespace);
		Value object = sesameValueFactory.createURI(s);
		
		model.add(subject, predicate, object, context);
		
		//Set Title
		predicate = sesameValueFactory.createURI(cworkNamespace + "title");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords(this.cwEntity.getLabel(), 10, false, false));
		
		model.add(subject, predicate, object, context);

		//Set Short Title
		predicate = sesameValueFactory.createURI(cworkNamespace + "shortTitle");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords("", 10, false, false));
		
		model.add(subject, predicate, object, context);

		//Set Category
		predicate = sesameValueFactory.createURI(cworkNamespace + "category");
		object = sesameValueFactory.createURI(ru.stringURI("category", cwEntity.getCategory(), false, false));

		model.add(subject, predicate, object, context);
		
		//Set Description
		predicate = sesameValueFactory.createURI(cworkNamespace + "description");
		object = sesameValueFactory.createLiteral(ru.sentenceFromDictionaryWords("", ru.nextInt(8, 26), false, false));
		
		model.add(subject, predicate, object, context);
		
		boolean initialAboutUriUsed = false;
		String initialUri = this.cwEntity.getObjectFromTriple(Entity.ENTITY_ABOUT);
		
		if (usePresetAboutTagUri) {
			initialUri = presetAboutTagUri;
		}
		
		//Set About(s)
		//using aboutsCount + 1, because Definitions.aboutsAllocations.getAllocation() returning 0 is still a valid allocation
		for (int i = 0; i < aboutsCount + 1; i++) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "about");
			
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
			} else {
				initialUri = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI();
			}
			
			object = sesameValueFactory.createURI(initialUri.replace("<", "").replace(">", ""));
			
			model.add(subject, predicate, object, context);
		}
		
		//Set Mention(s)
		//using mentionsCount + 1, because Definitions.mentionsAllocations.getAllocation() returning 0 is still a valid allocation
		boolean geonamesLocationUsedLocal = false;			
		for (int i = 0; i < mentionsCount + 1; i++) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "mentions");
			
			if (!initialAboutUriUsed) {
				initialAboutUriUsed = true;
			} else {
				if (!geonamesLocationUsedLocal) {
					geonamesLocationUsedLocal = true;
					initialUri = DataManager.geonamesIdsList.get(ru.nextInt(DataManager.geonamesIdsList.size()));
				} else {
					initialUri = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size())).getURI();
				}
			}
			
			object = sesameValueFactory.createURI(initialUri.replace("<", "").replace(">", ""));			
			
			model.add(subject, predicate, object, context);
		}
		
		switch (cwType) {
		case BLOG_POST :
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "InternationalAudience");
			
			model.add(subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(false);
			
			model.add(subject, predicate, object, context);
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "TextualFormat");
			
			model.add(subject, predicate, object, context);
			
			if (ru.nextBoolean()) {
				//Set additional primary format randomly
				predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
				object = sesameValueFactory.createURI(cworkNamespace + "InteractiveFormat");
				
				model.add(subject, predicate, object, context);
			}
			
			break;
		case NEWS_ITEM :
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "NationalAudience");
			
			model.add(subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(false);
			
			model.add(subject, predicate, object, context);			
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "TextualFormat");
			
			model.add(subject, predicate, object, context);			
			
			//Set additional primary format
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			object = sesameValueFactory.createURI(cworkNamespace + "InteractiveFormat");
			
			model.add(subject, predicate, object, context);
			
			break;
		case PROGRAMME : 
			//Set Audience
			predicate = sesameValueFactory.createURI(cworkNamespace + "audience");
			object = sesameValueFactory.createURI(cworkNamespace + "InternationalAudience");
			
			model.add(subject, predicate, object, context);
			
			//Set LiveCoverage
			predicate = sesameValueFactory.createURI(cworkNamespace + "liveCoverage");
			object = sesameValueFactory.createLiteral(true);
			
			model.add(subject, predicate, object, context);			
			
			//Set PrimaryFormat
			predicate = sesameValueFactory.createURI(cworkNamespace + "primaryFormat");
			if (ru.nextBoolean()) {
				object = sesameValueFactory.createURI(cworkNamespace + "VideoFormat");
			} else {
				object = sesameValueFactory.createURI(cworkNamespace + "AudioFormat");
			}
			
			model.add(subject, predicate, object, context);			
						
			break;
		}
		
		//Creation and Modification date
		Calendar calendar = Calendar.getInstance();
		
		if (usePresetDate) {
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateCreated");
			object = sesameValueFactory.createLiteral(presetDate);
			model.add(subject, predicate, object, context);
			
			//Set Modification Date
			calendar.setTime(presetDate);
			calendar.add(Calendar.MONTH, ru.nextInt(12));
			calendar.add(Calendar.DATE, ru.nextInt(31));
			calendar.add(Calendar.HOUR, ru.nextInt(24));			
			
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateModified");
			object = sesameValueFactory.createLiteral(calendar.getTime());
		} else	{
			Date randomDateTime = ru.randomDateTime();
			
			//Set Creation Date
			calendar.setTime(randomDateTime);
			calendar.add(Calendar.MONTH, -1 * ru.nextInt(12));
			calendar.add(Calendar.DATE, -1 * ru.nextInt(31));
			calendar.add(Calendar.HOUR, -1 * ru.nextInt(24));
			
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateCreated");
			object = sesameValueFactory.createLiteral(calendar.getTime());
			
			model.add(subject, predicate, object, context);
			
			//Set Modification Date
			predicate = sesameValueFactory.createURI(cworkNamespace + "dateModified");
			object = sesameValueFactory.createLiteral(randomDateTime);
		}
		
		model.add(subject, predicate, object, context);
		
		//Set Thumbnail
		predicate = sesameValueFactory.createURI(cworkNamespace + "thumbnail");
		object = sesameValueFactory.createURI(ru.randomURI("thumbnail", false, false));
		
		model.add(subject, predicate, object, context);
		
		//Set cwork:altText to thumbnail
		predicate = sesameValueFactory.createURI(cworkNamespace + "altText");
		object = sesameValueFactory.createLiteral("thumbnail atlText for CW " + adaptedContextUri);
		
		model.add(subject, predicate, object, context);
		
		//Set PrimaryContentOf
		int random = ru.nextInt(1, 4);
		for (int i = 0; i < random; i++) {
			predicate = sesameValueFactory.createURI(bbcNamespace + "primaryContentOf");
			String primaryContentUri = ru.randomURI("things", false, true);
			object = sesameValueFactory.createURI(primaryContentUri);
			
			model.add(subject, predicate, object, context);
			
			URI subjectPrimaryContent = sesameValueFactory.createURI(primaryContentUri);
			predicate = sesameValueFactory.createURI(bbcNamespace + "webDocumentType");
			if (ru.nextBoolean()) {
				object = sesameValueFactory.createURI(bbcNamespace + "HighWeb");
			} else {
				object = sesameValueFactory.createURI(bbcNamespace + "Mobile");
			}
			
			model.add(subjectPrimaryContent, predicate, object, context);
		}
		
		return model;
	}
}
