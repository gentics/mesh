package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldContainer;

/**
 * POJO for a node update request.
 */
public class NodeUpdateRequest implements FieldContainer {

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO 639-1 language tag of the node content.")
	private String language;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Dynamic map with fields of the node content.")
	private FieldMap fields = new FieldMapImpl();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version number which must be provided in order to handle and detect concurrent changes to the node content.")
	private String version;

	public NodeUpdateRequest() {
	}

	/**
	 * Return the language of the node.
	 * 
	 * @return Language tag of the language for the node
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node which should be updated.
	 * 
	 * @param language
	 *            Language tag for the node
	 */
	public NodeUpdateRequest setLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * Set the field which should be updated.
	 * 
	 * @return Fields of the node
	 */
	public FieldMap getFields() {
		return fields;
	}

	/**
	 * Set the fields which should be updated.
	 *
	 * @param fields
	 *            A field map containing all fields to be updated.
	 * @return this
	 */
	public NodeUpdateRequest setFields(FieldMap fields) {
		this.fields = fields;
		return this;
	}

	/**
	 * Get the version of the fields
	 * 
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version of the fields
	 * 
	 * @param version
	 */
	public NodeUpdateRequest setVersion(String version) {
		this.version = version;
		return this;
	}

}
