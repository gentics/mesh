package com.gentics.cailun.core.rest.project.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class ProjectResponse extends AbstractRestModel {

	private String name;

	public ProjectResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
