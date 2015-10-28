package com.gentics.mesh.core.rest.group;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO that is used group response models.
 */
public class GroupResponse extends AbstractGenericRestResponse {

	private String name;

	private List<String> roles = new ArrayList<>();

	public GroupResponse() {
	}

	/**
	 * Return the name of the group.
	 * 
	 * @return Name of the group
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the group.
	 * 
	 * @param name
	 *            Name of the group
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a list of roles which are assigned to the group.
	 * 
	 * @return List of roles
	 */
	public List<String> getRoles() {
		return roles;
	}

}
