package com.gentics.mesh.demo;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.user.HibUser;

/**
 * Container for user, group, role and password references of an user.
 */
public class UserInfo {

	private HibUser user;
	private String userUuid;

	private Group group;
	private String groupUuid;

	private Role role;
	private String roleUuid;

	private String password;

	public UserInfo(HibUser user, Group group, Role role, String password) {
		this.user = user;
		this.userUuid = user.getUuid();
		this.group = group;
		this.groupUuid = group.getUuid();
		this.role = role;
		this.roleUuid = role.getUuid();
		this.password = password;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Group getGroup() {
		return group;
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public Role getRole() {
		return role;
	}

	public String getRoleUuid() {
		return roleUuid;
	}

	public HibUser getUser() {
		return user;
	}

	public String getUserUuid() {
		return userUuid;
	}

	public String getPassword() {
		return password;
	}

}