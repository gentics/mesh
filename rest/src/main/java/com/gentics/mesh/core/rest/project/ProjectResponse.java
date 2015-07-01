package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class ProjectResponse extends AbstractRestModel {

	private String name;
	private String[] permissions = {};

	private String rootNodeUuid;

	public ProjectResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	public String getRootNodeUuid() {
		return rootNodeUuid;
	}

	public void setRootNodeUuid(String rootNodeUuid) {
		this.rootNodeUuid = rootNodeUuid;
	}

}
