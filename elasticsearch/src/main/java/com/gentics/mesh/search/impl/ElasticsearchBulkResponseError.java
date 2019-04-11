package com.gentics.mesh.search.impl;

import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ElasticsearchBulkResponseError extends Throwable implements ElasticsearchResponseErrorStreamable {
	private final JsonObject json;

	public ElasticsearchBulkResponseError(JsonObject json) {
		this.json = json;
	}

	public JsonObject getJson() {
		return json;
	}

	@Override
	public Stream<ElasticsearchResponseError> stream() {
		return json.getJsonArray("items").stream()
			.flatMap(obj -> ((JsonObject)obj).stream())
			.map(Map.Entry::getValue)
			.map(obj -> ((JsonObject) obj).getJsonObject("error"))
			.filter(Objects::nonNull)
			.map(ElasticsearchResponseError::new);
	}
}
