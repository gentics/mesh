package com.gentics.mesh.plugin.auth;

import java.util.List;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public class MappingResult {

	UserUpdateRequest user;
	List<RoleResponse> roles;
	List<GroupResponse> groups;

	public MappingResult() {

	}

	public UserUpdateRequest getUser() {
		return user;
	}

	public MappingResult setUser(UserUpdateRequest user) {
		this.user = user;
		return this;
	}

	public List<GroupResponse> getGroups() {
		return groups;
	}

	public MappingResult setGroups(List<GroupResponse> groups) {
		this.groups = groups;
		return this;
	}

	public List<RoleResponse> getRoles() {
		return roles;
	}

	public MappingResult setRoles(List<RoleResponse> roles) {
		this.roles = roles;
		return this;
	}

}
