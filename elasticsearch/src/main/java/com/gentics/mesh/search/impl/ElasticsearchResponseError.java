package com.gentics.mesh.search.impl;

import io.vertx.core.json.JsonObject;

import java.util.stream.Stream;

/**
 * Response error for failed ES queries. 
 */
public class ElasticsearchResponseError extends Throwable implements ElasticsearchResponseErrorStreamable {

	private final JsonObject json;
	private final String actionType;

	public ElasticsearchResponseError(JsonObject json, String actionType) {
		super(json.getString("reason"));
		this.json = json;
		this.actionType = actionType;
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

	/**
	 * Can be index, create, delete or update. See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html"></a>
	 * 
	 * @return
	 */
	public String getActionType() {
		return actionType;
	}
}
