package com.gentics.mesh.core.rest.role;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.group.GroupReference;

/**
 * POJO for a role response model.
 */
public class RoleResponse extends AbstractGenericRestResponse {

	private String name;

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
	 */
	public void setName(String name) {
		this.name = name;
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
	 */
	public void setGroups(List<GroupReference> groups) {
		this.groups = groups;
	}

}
