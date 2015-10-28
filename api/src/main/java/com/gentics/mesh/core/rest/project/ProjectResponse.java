package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class ProjectResponse extends AbstractGenericRestResponse {

	private String name;

	private String rootNodeUuid;

	public ProjectResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRootNodeUuid() {
		return rootNodeUuid;
	}

	public void setRootNodeUuid(String rootNodeUuid) {
		this.rootNodeUuid = rootNodeUuid;
	}

}
