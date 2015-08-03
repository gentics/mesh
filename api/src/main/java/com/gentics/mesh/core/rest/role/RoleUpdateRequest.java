package com.gentics.mesh.core.rest.role;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Role update request model.
 */
public class RoleUpdateRequest implements RestModel {

	private String name;

	public RoleUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
