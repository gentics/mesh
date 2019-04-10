package com.gentics.mesh.search.impl;

import io.vertx.core.json.JsonObject;

import java.util.stream.Stream;

public class ElasticsearchResponseError extends Throwable implements ElasticsearchResponseErrorStreamable {
	private final JsonObject json;

	public ElasticsearchResponseError(JsonObject json) {
		this.json = json;
	}

	public String getType() {
		return json.getString("type");
	}

	public String getReason() {
		return json.getString("reason");
	}

	public JsonObject getJson() {
		return json;
	}

	@Override
	public Stream<ElasticsearchResponseError> stream() {
		return Stream.of(this);
	}
}
