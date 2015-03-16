package com.gentics.cailun.core.rest.role.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class RoleResponse extends AbstractRestModel {

	private String name;
	private String[] perms;

	// TODO add groups to response

	public RoleResponse() {
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

}
