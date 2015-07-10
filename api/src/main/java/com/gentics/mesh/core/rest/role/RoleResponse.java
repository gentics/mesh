package com.gentics.mesh.core.rest.role;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.group.GroupResponse;

public class RoleResponse extends AbstractGenericNodeRestModel {

	private String name;

	private List<GroupResponse> groups = new ArrayList<>();

	public RoleResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<GroupResponse> getGroups() {
		return groups;
	}

	public void setGroups(List<GroupResponse> groups) {
		this.groups = groups;
	}

}
