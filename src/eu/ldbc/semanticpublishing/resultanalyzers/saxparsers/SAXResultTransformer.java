package eu.ldbc.semanticpublishing.resultanalyzers.saxparsers;

import java.io.InputStream;

/**
 * Provides a common method transform() for SAX Parser implementations,
 * used mainly for transforming results from service queries.
 */
public interface SAXResultTransformer {
	public void transform(InputStream is);
}
