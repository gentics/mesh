package com.gentics.mesh.core.rest.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a role request model that is used for role creation.
 */
public class RoleCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("New name of the role")
	private String name;

	public RoleCreateRequest() {
	}

	/**
	 * Return the name of the role.
	 * 
	 * @return Name of the role
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the role.
	 * 
	 * @param name
	 *            Name of the role
	 * @return Fluent API
	 */
	public RoleCreateRequest setName(String name) {
		this.name = name;
		return this;
	}
}
