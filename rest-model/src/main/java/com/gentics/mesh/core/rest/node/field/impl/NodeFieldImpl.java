package com.gentics.mesh.core.rest.node.field.impl;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;

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
}
