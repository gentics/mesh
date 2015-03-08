package com.gentics.cailun.core.rest.group.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class GroupResponse extends AbstractRestModel {

	private String name;

	private List<String> childGroups = new ArrayList<>();

	private List<String> roles = new ArrayList<>();

	private List<String> users = new ArrayList<>();

	public GroupResponse() {
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
