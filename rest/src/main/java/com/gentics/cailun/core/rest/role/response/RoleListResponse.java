package com.gentics.cailun.core.rest.role.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestListResponse;

public class RoleListResponse extends AbstractRestListResponse{

	private List<RoleResponse> roles = new ArrayList<>();

	public RoleListResponse() {
	}

	public void addRole(RoleResponse role) {
		this.roles.add(role);
	}

	public List<RoleResponse> getRoles() {
		return roles;
	}

}
