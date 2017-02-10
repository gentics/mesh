package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * @see Schema
 */
public class SchemaModel extends SchemaUpdateRequest implements Schema {

	@JsonPropertyDescription("Name of the display field.")
	private String displayField;

	@JsonPropertyDescription("Name of the segment field. This field is used to construct the webroot path to the node.")
	private String segmentField;

	@JsonPropertyDescription("Flag which indicates whether nodes which use this schema store additional child nodes.")
	private boolean container = false;

	/**
	 * Create a new schema with the given name.
	 * 
	 * @param name
	 */
	public SchemaModel(String name) {
		setName(name);
	}

	public SchemaModel() {
	}

}
