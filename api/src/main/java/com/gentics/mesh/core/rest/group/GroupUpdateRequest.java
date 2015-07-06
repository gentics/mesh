package com.gentics.mesh.core.rest.group;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class GroupUpdateRequest extends AbstractRestModel {

	private String name;

	public GroupUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
