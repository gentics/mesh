package com.gentics.mesh.plugin.auth;

import java.util.List;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

/**
 * The mapping result can be used in combination with plugins which implement {@link AuthServicePlugin}. The result contains information about which groups and
 * roles should be synced in Gentics Mesh once an OAuth2 request is being processed. The filter functions can be used to remove older group->role and
 * group->user assignments. The detailed process on how this information is used is described in the {@link AuthServicePlugin} documentation.
 * 
 * Roles which are listed in the {@link GroupResponse} will automatically be loaded and assigned to the group. 
 */
public class MappingResult {

	/**
	 * Mapped user information which will be used to update the user.
	 */
	private UserUpdateRequest user;

	/**
	 * List of roles which will be created during the sync.
	 */
	private List<RoleResponse> roles;

	/**
	 * List of groups which will be created during the sync.
	 */
	private List<GroupResponse> groups;

	/**
	 * Filter function which can be used to remove older roles from mapped groups.
	 */
	private RoleFilter roleFilter;

	/**
	 * Filter function which can be used to remove older groups from the synced user.
	 */
	private GroupFilter groupFilter;

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

	public GroupFilter getGroupFilter() {
		return groupFilter;
	}

	public MappingResult setGroupFilter(GroupFilter groupFilter) {
		this.groupFilter = groupFilter;
		return this;
	}

	public RoleFilter getRoleFilter() {
		return roleFilter;
	}

	public MappingResult setRoleFilter(RoleFilter roleFilter) {
		this.roleFilter = roleFilter;
		return this;
	}

}
