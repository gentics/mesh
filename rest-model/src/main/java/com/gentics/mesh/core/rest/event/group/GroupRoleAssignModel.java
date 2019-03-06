package com.gentics.mesh.core.rest.event.group;

import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.role.RoleReference;

public class GroupRoleAssignModel extends AbstractMeshEventModel {

	private GroupReference group;
	private RoleReference role;

	public GroupRoleAssignModel() {
	}

	public GroupReference getGroup() {
		return group;
	}

	public void setGroup(GroupReference group) {
		this.group = group;
	}

	public RoleReference getRole() {
		return role;
	}

	public void setRole(RoleReference role) {
		this.role = role;
	}

}
