package com.gentics.mesh.plugin.export.model;

import io.vertx.core.json.JsonObject;

import java.util.List;

public class ResponseData {
	private List<JsonObject> elements;
	private boolean foundElements;
	private boolean hasMore;

	public ResponseData() {}

	public List<JsonObject> getElements() {
		return elements;
	}

	public ResponseData setElements(List<JsonObject> elements) {
		this.elements = elements;
		return this;
	}

	public boolean isFoundElements() {
		return foundElements;
	}

	public ResponseData setFoundElements(boolean foundElements) {
		this.foundElements = foundElements;
		return this;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public ResponseData setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
		return this;
	}
}
