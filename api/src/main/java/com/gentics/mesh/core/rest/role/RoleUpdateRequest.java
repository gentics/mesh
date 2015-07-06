package com.gentics.mesh.core.rest.role;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class RoleUpdateRequest extends AbstractRestModel {

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
