package com.gentics.mesh.core.field.json;

import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.rest.node.field.JsonContent;

import io.vertx.core.json.JsonObject;

public interface JsonFieldTestHelper {

	static final String EXAMPLE_DATE = "2011-12-03T10:15:30Z";

	static final DataProvider FILLTEXT = (container, name) -> container.createJson(name).setJson(make("whatever"));
	static final DataProvider CREATE_EMPTY = (container, name) -> container.createJson(name).setJson(null);
	static final FieldFetcher FETCH = (container, name) -> container.getJson(name);

	public static JsonContent make(String content) {
		return new JsonContent().setObject(new JsonObject().put("content", content));
	}
}
