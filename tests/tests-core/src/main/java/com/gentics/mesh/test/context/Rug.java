package com.gentics.mesh.test.context;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * Role + User + Group (RUG)
 */
public class Rug {
	private final UserResponse user;
	private final GroupResponse group;
	private final RoleResponse role;


	public Rug(UserResponse user, GroupResponse group, RoleResponse role) {
		this.user = user;
		this.group = group;
		this.role = role;
	}

	public UserResponse getUser() {
		return user;
	}

	public GroupResponse getGroup() {
		return group;
	}

	public RoleResponse getRole() {
		return role;
	}
}
