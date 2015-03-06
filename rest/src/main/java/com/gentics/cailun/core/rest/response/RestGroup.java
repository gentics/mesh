package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestGroup extends AbstractRestModel {

	private String name;

	private List<RestGroup> childGroups = new ArrayList<>();

	private List<RestRole> roles = new ArrayList<>();

	private List<RestUserResponse> users = new ArrayList<>();

	public RestGroup() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<RestGroup> getChildGroups() {
		return childGroups;
	}

	public void setChildGroups(List<RestGroup> childGroups) {
		this.childGroups = childGroups;
	}

	public List<RestRole> getRoles() {
		return roles;
	}

	public void setRoles(List<RestRole> roles) {
		this.roles = roles;
	}

	public List<RestUserResponse> getUsers() {
		return users;
	}

	public void setUsers(List<RestUserResponse> users) {
		this.users = users;
	}

}
