package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a tag create request model.
 */
public class TagFamilyCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the tag family which will be created.")
	private String name;

	public TagFamilyCreateRequest() {
	}

	/**
	 * Set the name of the tag.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the tag.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
}
