package com.gentics.mesh.search;

import java.util.Iterator;
import java.util.List;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.etc.config.search.IndexSearchMode;

import io.vertx.core.json.JsonObject;

/**
 * A config-dependent index search iterator.
 */
public class IndexSearchIteratorWrapper implements Iterator<JsonObject> {

	private final AbstractIndexSearchIterator wrapped;

	public IndexSearchIteratorWrapper(ElasticSearchOptions options, ElasticsearchClient<JsonObject> client, JsonObject scrollResp, List<String> indices) {
		if (IndexSearchMode.SCROLL.equals(options.getIndexSearchMode())) {
			wrapped = new ScrollingIterator(client, scrollResp);
		} else {
			wrapped = new SearchAfterIterator(client, scrollResp, indices);
		}
	}

	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	@Override
	public JsonObject next() {
		return wrapped.next();
	}
}
