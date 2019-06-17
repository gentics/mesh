package com.gentics.mesh.search.impl;

import com.gentics.elasticsearch.client.okhttp.ElasticsearchOkClient;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;

import io.vertx.core.json.JsonObject;

public class SearchClient extends ElasticsearchOkClient<JsonObject> {

	public SearchClient(String scheme, String hostname, int port) {
		super(scheme, hostname, port);
		setConverterFunction(JsonObject::new);
	}

	/**
	 * Invoke a scroll the request.
	 * 
	 * @param scrollId
	 * @param scrollTimeout
	 * @return
	 */
	public RequestBuilder<JsonObject> scroll(String scrollId, String scrollTimeout) {
		JsonObject request = new JsonObject();
		request.put("scroll", scrollTimeout);
		request.put("scroll_id", scrollId);
		return postBuilder("_search/scroll", request);
	}

}
