package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.json.JsonUtil;

/**
 * @see JsonField
 */
public class JsonFieldImpl implements JsonField {

	@JsonPropertyDescription("JSON field value")
	private JsonNode json;

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

	@Override
	public JsonNode getJson() {
		return json;
	}

	@Override
	public JsonField setJson(JsonNode json) {
		this.json = json;
		return this;
	}

	@Override
	public String toString() {
		return JsonUtil.toJson(getJson());
	}
}
