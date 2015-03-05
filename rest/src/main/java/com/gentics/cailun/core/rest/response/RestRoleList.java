package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestRoleList {

	private List<RestRole> roles = new ArrayList<>();

	public RestRoleList() {
	}

	public void addRole(RestRole role) {
		this.roles.add(role);
	}

	public List<RestRole> getRoles() {
		return roles;
	}

}
