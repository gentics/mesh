package com.gentics.mesh.core.rest.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.group.GroupReference;

/**
 * POJO for a role response model.
 */
public class RoleResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the role.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of groups which are assigned to the role.")
	private List<GroupReference> groups = new ArrayList<>();

	public RoleResponse() {
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
	public RoleResponse setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return a list of group references to which the role was assigned.
	 * 
	 * @return List of groups to which the role was assigned
	 */
	public List<GroupReference> getGroups() {
		return groups;
	}

	/**
	 * Set a list of group references to which the role was assigned.
	 * 
	 * @param groups
	 *            List of groups to which the role was assigned
	 * @return Fluent API
	 */
	@JsonProperty("groups")
	public RoleResponse setGroups(List<GroupReference> groups) {
		this.groups = groups;
		return this;
	}

	/**
	 * Set the groups for the role.
	 * 
	 * @param groups
	 * @return Fluent API
	 */
	@JsonIgnore
	public RoleResponse setGroups(GroupReference... groups) {
		this.groups = Arrays.asList(groups);
		return this;
	}

}
