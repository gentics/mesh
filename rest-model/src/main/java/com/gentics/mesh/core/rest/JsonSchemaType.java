package com.gentics.mesh.core.rest;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Serializable JSON type container.
 */
public class JsonSchemaType implements RestModel {

	private String type = "object";

	public JsonSchemaType() {
	}

	public JsonSchemaType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public JsonSchemaType setType(String type) {
		this.type = type;
		return this;
	}
}
