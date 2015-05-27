package com.gentics.mesh.core.rest.project.response;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;

public class ProjectResponse extends AbstractRestModel {

	private String name;
	private String[] perms = {};

	private String rootNodeUuid;

	public ProjectResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

	public String getRootNodeUuid() {
		return rootNodeUuid;
	}

	public void setRootNodeUuid(String rootNodeUuid) {
		this.rootNodeUuid = rootNodeUuid;
	}

}
