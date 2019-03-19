package com.gentics.mesh.core.rest.event.group;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.role.RoleReference;

public class GroupRoleAssignModel extends AbstractMeshEventModel {

	private GroupReference group;
	private RoleReference role;

	@JsonCreator
	public GroupRoleAssignModel(String origin, EventCauseInfo cause, MeshEvent event, GroupReference group, RoleReference role) {
		super(origin, cause, event);
		this.group = group;
		this.role = role;
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
