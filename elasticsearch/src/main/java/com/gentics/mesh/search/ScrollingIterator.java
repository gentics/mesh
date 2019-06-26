package com.gentics.mesh.search;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Wrapper for typical elasticsearch {@link SearchResponse} results. The wrapper will use the scroll API to advance the iteration if needed.
 */
public class ScrollingIterator implements Iterator<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(ScrollingIterator.class);

	private Iterator<JsonObject> currentIterator;
	private JsonObject currentResponse;
	private ElasticsearchClient<JsonObject> client;

	/**
	 * Create a new iterator.
	 * 
	 * @param client
	 *            Elasticsearch client used to invoke additional queries
	 * @param scrollResp
	 *            Current scroll which will provide the initial results and the scroll reference id.
	 */
	public ScrollingIterator(ElasticsearchClient<JsonObject> client, JsonObject scrollResp) {
		// TODO add type check
		this.currentIterator = scrollResp.getJsonObject("hits").getJsonArray("hits").stream().map(o -> (JsonObject) o).iterator();
		this.currentResponse = scrollResp;
		this.client = client;
	}

	@Override
	public boolean hasNext() {
		if (currentIterator.hasNext()) {
			return true;
		}

		if (currentResponse.getJsonObject("hits").getJsonArray("hits").size() == 0) {
			return false;
		} else {
			advanceScroll();
			return currentIterator.hasNext();
		}
	}

	/**
	 * Load a new search response using the scrollId of the previous scroll.
	 */
	private void advanceScroll() {
		JsonObject json = new JsonObject();
		json.put("scroll_id", currentResponse.getString("_scroll_id"));
		json.put("scroll", "1m");
		try {
			RequestBuilder<JsonObject> scrollRequest = client.searchScroll(json, null);
			currentResponse = scrollRequest.sync();
		} catch (HttpErrorException e) {
			System.out.println(e.toString());
			log.error("Error while handling scroll request.", e);
			throw new RuntimeException("Error while handling scroll request", e);
		}
		currentIterator = currentResponse.getJsonObject("hits").getJsonArray("hits").stream().map(o -> (JsonObject) o).iterator();
	}

	@Override
	public JsonObject next() {
		// We need to invoke the hasNext method in order to advance the iterator if needed.
		if (hasNext()) {
			return currentIterator.next();
		} else {
			throw new NoSuchElementException();
		}
	}

}
