package com.gentics.mesh.core.rest.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO that is used for group update models.
 */
public class GroupUpdateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("New name of the group")
	private String name;

	public GroupUpdateRequest() {
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
	 * Set the new name of the group.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

}
