package com.gentics.mesh.core.rest.group;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class GroupResponse extends AbstractRestModel {

	private String name;

	private List<String> roles = new ArrayList<>();

	private String[] perms = {};

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

	public String[] getPerms() {
		return perms;
	}

	public void setPerms(String... perms) {
		this.perms = perms;
	}

}
