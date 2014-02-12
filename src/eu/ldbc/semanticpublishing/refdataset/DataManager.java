package eu.ldbc.semanticpublishing.refdataset;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import eu.ldbc.semanticpublishing.refdataset.model.Entity;

/**
 * A class for storing important to the benchmark data e.g.
 *   - next available ID for a CreativeWork (greatest number)
 *   - list of tagged popular and regular entities
 */
public class DataManager {
		
	//a list for popular entities 
	public static final ArrayList<Entity> popularEntitiesList = new ArrayList<Entity>();
	
	//a list for regular entities
	public static final ArrayList<Entity> regularEntitiesList = new ArrayList<Entity>();
	
	//a list for all entities that have been tagged in a Creative Work
	public static final ArrayList<String> taggedEntityUrisList = new ArrayList<String>();
	
	//a list of all geonames ids taken from reference dataset
	public static final ArrayList<String> geonamesIdsList = new ArrayList<String>();	

	//stores the ID of a Creative Work which is has the greatest value, used for further CRUD operations
	public static AtomicLong creativeWorksNexId = new AtomicLong(0);
}
