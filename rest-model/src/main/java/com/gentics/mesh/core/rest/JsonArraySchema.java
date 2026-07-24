package com.gentics.mesh.core.rest;

/**
 * JSON schema for arrays.
 */
public class JsonArraySchema extends JsonSchemaType implements JsonSchema {

	private JsonSchemaType items;

	public JsonArraySchema() {
		super("array");
	}

	public JsonArraySchema(JsonSchemaType items) {
		this();
		this.items = items;
	}

	public JsonSchemaType getItems() {
		return items;
	}

	public JsonArraySchema setItems(JsonSchemaType items) {
		this.items = items;
		return this;
	}
}
