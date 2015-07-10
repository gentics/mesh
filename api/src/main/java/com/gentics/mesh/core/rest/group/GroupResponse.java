package com.gentics.mesh.core.rest.group;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

public class GroupResponse extends AbstractGenericNodeRestModel {

	private String name;

	private List<String> roles = new ArrayList<>();

	public GroupResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

}
