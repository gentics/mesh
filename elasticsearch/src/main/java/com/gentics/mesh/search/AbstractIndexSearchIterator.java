package com.gentics.mesh.search;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.gentics.elasticsearch.client.ElasticsearchClient;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Base class for the index search method implementations.
 */
public abstract class AbstractIndexSearchIterator implements Iterator<JsonObject> {

	protected Iterator<JsonObject> currentIterator;
	protected JsonObject currentResponse;
	protected final ElasticsearchClient<JsonObject> client;

	public AbstractIndexSearchIterator(ElasticsearchClient<JsonObject> client, JsonObject searchResp) {
		this.currentIterator = searchResp.getJsonObject("hits").getJsonArray("hits").stream().map(o -> (JsonObject) o).iterator();
		this.currentResponse = searchResp;
		this.client = client;
	}

	@Override
	public boolean hasNext() {
		if (currentIterator.hasNext()) {
			return true;
		}

		JsonArray hits = currentResponse.getJsonObject("hits").getJsonArray("hits");
		if (hits.size() == 0) {
			return false;
		} else {
			advanceSearch(hits);
			return currentIterator.hasNext();
		}
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

	/**
	 * Search advance implementation.
	 * 
	 * @param hits
	 */
	protected abstract void advanceSearch(JsonArray hits);
}
