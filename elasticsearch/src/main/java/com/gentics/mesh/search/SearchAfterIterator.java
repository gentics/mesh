package com.gentics.mesh.search;

import java.util.List;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Wrapper for typical elasticsearch {@link SearchResponse} results. The wrapper will use `search_after` parameters to advance the iteration if needed.
 */
class SearchAfterIterator extends AbstractIndexSearchIterator {

	private static final Logger log = LoggerFactory.getLogger(SearchAfterIterator.class);

	private final List<String> indices;

	/**
	 * Create a new iterator.
	 * 
	 * @param client Elasticsearch client used to invoke additional queries
	 * @param searchResp Current search which will provide the initial results and the sort IDs.
	 */
	public SearchAfterIterator(ElasticsearchClient<JsonObject> client, JsonObject searchResp, List<String> indices) {
		super(client, searchResp);
		this.indices = indices;
	}

	/**
	 * Load a new search response using the search_after, made of the sort IDs of the previous search.
	 * @param sort 
	 */
	@Override
	protected void advanceSearch(JsonArray hits) {
		JsonObject json = new JsonObject();
		json.put("search_after", hits.getJsonObject(hits.size()-1).getJsonArray("sort"));
		try {
			RequestBuilder<JsonObject> searchRequest = client.search(json, indices);
			currentResponse = searchRequest.sync();
		} catch (HttpErrorException e) {
			log.error("Error while handling search_after request.", e);
			throw new RuntimeException("Error while handling search_after request", e);
		}
		currentIterator = currentResponse.getJsonObject("hits").getJsonArray("hits").stream().map(o -> (JsonObject) o).iterator();
	}
}
