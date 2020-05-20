package com.gentics.mesh.core.rest.node.field.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

/**
 * @see NodeField
 */
public class NodeFieldImpl implements NodeField {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the referenced node.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the referenced node.")
	private String path;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Set of language paths that the node provides.")
	private Map<String, String> languagePaths;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the node.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference schema;

	@Override
	public String getUuid() {
		return uuid;
	}

	public NodeField setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

	@Override
	public String getPath() {
		return path;
	}

	/**
	 * Set the webroot path
	 *
	 * @param path
	 *            webroot path
	 * @return this instance
	 */
	public NodeField setPath(String path) {
		this.path = path;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

	@Override
	public Map<String, String> getLanguagePaths() {
		return languagePaths;
	}

	/**
	 * Set the language specific webroot paths.
	 *
	 * @param languagePaths
	 */
	public NodeField setLanguagePaths(Map<String, String> languagePaths) {
		this.languagePaths = languagePaths;
		return this;
	}

	/**
	 * Set the schema reference of the node.
	 *
	 * @param schema
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	/**
	 * Return the schema reference of the node.
	 *
	 * @return
	 */
	public SchemaReference getSchema() {
		return schema;
	}
}
