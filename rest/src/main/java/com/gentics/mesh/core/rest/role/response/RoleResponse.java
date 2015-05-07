package com.gentics.mesh.core.rest.role.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.group.response.GroupResponse;

public class RoleResponse extends AbstractRestModel {

	private String name;
	private String[] perms = {};

	private List<GroupResponse> groups = new ArrayList<>();

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

	public List<GroupResponse> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupResponse> groups) {
		this.groups = groups;
	}

}
