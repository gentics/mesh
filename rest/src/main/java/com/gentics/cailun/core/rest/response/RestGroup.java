package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestGroup extends AbstractRestModel {

	private String name;

	private List<String> childGroups = new ArrayList<>();

	private List<String> roles = new ArrayList<>();

	private List<String> users = new ArrayList<>();

	public RestGroup() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getChildGroups() {
		return childGroups;
	}

	public void setChildGroups(List<String> childGroups) {
		this.childGroups = childGroups;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public List<String> getUsers() {
		return users;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}

}
