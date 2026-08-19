package com.gentics.mesh.core.rest.node.field;

import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;

/**
 * REST POJO for the JSON formatted information.
 */
public interface JsonField extends ListableField, MicroschemaListableField {

	/**
	 * Get the stored JSON object
	 * 
	 * @return
	 */
	JsonContent getJson();

	/**
	 * Store the given JSON object
	 * 
	 * @param json
	 * @return
	 */
	JsonField setJson(JsonContent json);

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
	static JsonField of(JsonContent json) {
		return new JsonFieldImpl().setJson(json);
	}
}
