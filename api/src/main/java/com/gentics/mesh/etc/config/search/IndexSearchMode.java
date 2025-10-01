package com.gentics.mesh.etc.config.search;

/**
 * The index search approach, that Mesh will use in Elasticsearch 
 */
public enum IndexSearchMode {
	/**
	 * Stateful search via Scroll API, using scroll IDs
	 */
	SCROLL,
	/**
	 * Stateless search with combination of `sort` and `search_after`
	 */
	SEARCH_AFTER
}
