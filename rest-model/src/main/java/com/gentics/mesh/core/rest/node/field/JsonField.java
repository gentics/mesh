package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;

import io.vertx.core.json.JsonObject;

/**
 * REST POJO for the JSON formatted information.
 */
public interface JsonField extends ListableField, MicroschemaListableField {

	/**
	 * Get the stored JSON object
	 * 
	 * @return
	 */
	JsonObject getJson();

	/**
	 * Store the given JSON object
	 * 
	 * @param json
	 * @return
	 */
	JsonField setJson(JsonObject json);

	@Override
	default Object getValue() {
		return getJson();
	}

	/**
	 * Shortcut for the fast REST model field creation.
	 * 
	 * @param json
	 * @return
	 */
	static JsonField of(JsonObject json) {
		return new JsonFieldImpl().setJson(json);
	}
}
