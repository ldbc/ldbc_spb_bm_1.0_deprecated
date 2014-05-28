package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.IOException;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXGeonamesTransformer;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;

/**
 * A class used to extract geonames ids from provided reference dataset for Great Britain.
 * Executes a sparql query and parses the result using an implementation of the SAXResultTransformer interface.
 */
public class GeonamesAnalyzer {
	private SparqlQueryExecuteManager sparqlQeuryManager;
	private MustacheTemplatesHolder queryTemplatesHolder;
	
	public GeonamesAnalyzer(SparqlQueryExecuteManager sparqlQueryExecuteManager, MustacheTemplatesHolder queryTemplatesHolder) {
		this.sparqlQeuryManager = sparqlQueryExecuteManager;
		this.queryTemplatesHolder = queryTemplatesHolder;
	}
	
	public ArrayList<String> collectGeonamesIds() throws IOException {
		StringBuilder query = new StringBuilder(); 
		query.append(queryTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.SYSTEM).get("analyzegeonamesdata.txt"));

		SAXGeonamesTransformer geonamesTransformer = new SAXGeonamesTransformer();
		sparqlQeuryManager.executeSystemQuery(geonamesTransformer, query.toString(), QueryType.SELECT);
		return geonamesTransformer.getGeonamesIds();
	}	
}
