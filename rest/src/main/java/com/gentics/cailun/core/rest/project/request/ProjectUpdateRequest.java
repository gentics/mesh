package com.gentics.cailun.core.rest.project.request;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;


public class ProjectUpdateRequest extends AbstractRestModel {

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
