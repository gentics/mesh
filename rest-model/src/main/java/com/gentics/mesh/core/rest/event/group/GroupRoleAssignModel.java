package com.gentics.mesh.core.rest.event.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * POJO for group<->role assignment events.
 */
public class GroupRoleAssignModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the group.")
	private GroupReference group;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the role that was assigned/unassigned from/to the group.")
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
