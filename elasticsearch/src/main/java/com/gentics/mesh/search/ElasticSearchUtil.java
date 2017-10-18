package com.gentics.mesh.search;

import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;

/**
 * Provides utility functions for elastic search
 */
public class ElasticSearchUtil {

	/**
	 * Parses an Elasticsearch query string to the SearchSourceBuilder object, which is used to send queries to ES.
	 * @param query The query to be sent to ES
	 * @return The SearchSourceBuilder object
	 * @throws IOException if the query could not be parsed
	 */
	public static SearchSourceBuilder parseQuery(String query) throws IOException {
		return SearchSourceBuilder.fromXContent(new QueryParseContext(XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, query)));
	}
}
