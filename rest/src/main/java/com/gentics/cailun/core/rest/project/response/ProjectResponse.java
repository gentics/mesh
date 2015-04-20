package com.gentics.cailun.core.rest.project.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class ProjectResponse extends AbstractRestModel {

	private String name;
	private String[] perms = {};

	private String rootTagUuid;

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

	public String getRootTagUuid() {
		return rootTagUuid;
	}

	public void setRootTagUuid(String rootTagUuid) {
		this.rootTagUuid = rootTagUuid;
	}

}
