package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.JsonField;

import io.vertx.core.json.JsonObject;

/**
 * @see JsonField
 */
public class JsonFieldImpl implements JsonField {

	@JsonPropertyDescription("JSON field value")
	private JsonObject json;

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

	@Override
	public JsonObject getJson() {
		return json;
	}

	@Override
	public JsonField setJson(JsonObject json) {
		this.json = json;
		return this;
	}

}
