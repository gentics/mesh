package com.gentics.mesh.core.rest.role;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.core.rest.group.GroupResponse;

public class RoleResponse extends AbstractRestModel {

	private String name;
	private String[] permissions = {};

	private List<GroupResponse> groups = new ArrayList<>();

	public RoleResponse() {
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

	public List<GroupResponse> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupResponse> groups) {
		this.groups = groups;
	}

}
