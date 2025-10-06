package com.gentics.mesh.search;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for typical elasticsearch {@link SearchResponse} results. The wrapper will use the scroll API to advance the iteration if needed.
 * 
 * @deprecated This implementation will be removed from public usage. Use {@link IndexSearchIteratorWrapper} instead.
 */
@Deprecated
public class ScrollingIterator extends AbstractIndexSearchIterator {

	private static final Logger log = LoggerFactory.getLogger(ScrollingIterator.class);

	/**
	 * Create a new iterator.
	 * 
	 * @param client
	 *            Elasticsearch client used to invoke additional queries
	 * @param scrollResp
	 *            Current scroll which will provide the initial results and the scroll reference id.
	 */
	public ScrollingIterator(ElasticsearchClient<JsonObject> client, JsonObject scrollResp) {
		super(client, scrollResp);
	}

	/**
	 * Load a new search response using the scrollId of the previous scroll.
	 */
	@Override
	protected void advanceSearch(JsonArray hits) {
		JsonObject json = new JsonObject();
		json.put("scroll_id", currentResponse.getString("_scroll_id"));
		json.put("scroll", "1m");
		try {
			RequestBuilder<JsonObject> scrollRequest = client.searchScroll(json, null);
			currentResponse = scrollRequest.sync();
		} catch (HttpErrorException e) {
			log.error("Error while handling scroll request.", e);
			throw new RuntimeException("Error while handling scroll request", e);
		}
		currentIterator = currentResponse.getJsonObject("hits").getJsonArray("hits").stream().map(o -> (JsonObject) o).iterator();
	}
}
