package com.gentics.mesh.core.rest.role;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a role update request model.
 */
public class RoleUpdateRequest implements RestModel {

	private String name;

	public RoleUpdateRequest() {
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

}
