package com.gentics.mesh.search.impl;

import com.gentics.elasticsearch.client.okhttp.ElasticsearchOkClient;

import io.vertx.core.json.JsonObject;

public class SearchClient extends ElasticsearchOkClient<JsonObject> {

	public SearchClient(String scheme, String hostname, int port) {
		super(scheme, hostname, port);
		setConverterFunction(JsonObject::new);
	}

}
