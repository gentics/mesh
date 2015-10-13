package com.gentics.mesh.demo;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.Role;

/**
 * Container for user, group, role and password references of an user.
 */
public class UserInfo {

	private User user;
	private Group group;
	private Role role;
	private String password;

	public UserInfo(User user, Group group, Role role, String password) {
		this.user = user;
		this.group = group;
		this.role = role;
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

	public Role getRole() {
		return role;
	}

	public User getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

}