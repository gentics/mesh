package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.json.JsonUtil;

/**
 * @see JsonField
 */
public class JsonFieldImpl implements JsonField {

	@JsonPropertyDescription("JSON field value")
	private JsonContent json;

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

	@Override
	public JsonContent getJson() {
		return json;
	}

	@Override
	public JsonField setJson(JsonContent json) {
		this.json = json;
		return this;
	}

	@Override
	public String toString() {
		return JsonUtil.toJson(getJson());
	}
}
