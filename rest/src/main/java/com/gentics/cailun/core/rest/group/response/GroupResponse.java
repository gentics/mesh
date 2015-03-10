package com.gentics.cailun.core.rest.group.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class GroupResponse extends AbstractRestModel {

	private String name;

	//TODO child groups must also have the information whether there are any childgroups for those groups. Otherwise in an ajax tree no arrow can be displayed
	private List<String> groups = new ArrayList<>();

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

	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(List<String> childGroups) {
		this.groups = childGroups;
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
