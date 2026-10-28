package com.gentics.mesh.plugin.search.service.query;

import com.gentics.mesh.plugin.search.service.QueryService;

/**
 * The query types supported by the {@link QueryService}.
 */
public enum QueryType {
	GraphQL(".graphql"),
	ElasticSearch(".es");

	private final String extension;

	/**
	 * Default constructor.
	 *
	 * @param extension The filename extension for the query type
	 */
	QueryType(String extension) {
		this.extension = extension;
	}

	/**
	 * Get the filename for a query of this type.
	 *
	 * @param queryName The name of the query file without extension
	 * @return The filename extension for the query type
	 */
	public String filename(String queryName) {
		return queryName + extension;
	}
}
