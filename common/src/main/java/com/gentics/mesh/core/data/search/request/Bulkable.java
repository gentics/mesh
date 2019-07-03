package com.gentics.mesh.core.data.search.request;

import io.reactivex.Single;

import java.util.List;

/**
 * A request that can be executed in a <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html">Bulk request</a>
 */
public interface Bulkable extends SearchRequest {
	/**
	 * Turns the request into a list of strings that will be sent to Elasticsearch.
	 * @return
	 */
	Single<List<String>> toBulkActions();

	/**
	 * Returns the length of the string of the encoded request.
	 * @return
	 */
	long bulkLength();
}
