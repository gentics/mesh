package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.common.RestModel;

public class ProjectUpdateRequest implements RestModel {

	private String name;

	public ProjectUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
