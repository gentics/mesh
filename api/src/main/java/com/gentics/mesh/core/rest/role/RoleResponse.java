package com.gentics.mesh.core.rest.role;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.group.GroupResponse;

/**
 * POJO for a role response model.
 */
public class RoleResponse extends AbstractGenericNodeRestModel {

	private String name;

	private List<GroupResponse> groups = new ArrayList<>();

	public RoleResponse() {
	}

	/**
	 * Return the name of the role.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the role.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return a list of groups to which the role was assigned.
	 * 
	 * @return
	 */
	public List<GroupResponse> getGroups() {
		return groups;
	}

	/**
	 * Set a list of groups to which the role was assigned.
	 * 
	 * @param groups
	 */
	public void setGroups(List<GroupResponse> groups) {
		this.groups = groups;
	}

}
