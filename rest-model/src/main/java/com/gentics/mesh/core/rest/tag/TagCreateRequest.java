package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for tag create requests.
 */
public class TagCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the tag which will be created.")
	private String name;

	public TagCreateRequest() {
	}

	/**
	 * Get the name of the tag.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the tag.
	 * 
	 * @param name
	 *            Name of the tag
	 * @return Fluent API
	 */
	public TagCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

}
