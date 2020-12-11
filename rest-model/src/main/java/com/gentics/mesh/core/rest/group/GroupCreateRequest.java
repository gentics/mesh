package com.gentics.mesh.core.rest.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST POJO for group request requests.
 */
public class GroupCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the group.")
	private String name;

	public GroupCreateRequest() {
	}

	/**
	 * Return the name of the group.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the group.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public GroupCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

}
