package com.gentics.mesh.core.rest.event.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.UserReference;

public class GroupUserAssignModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the group.")
	private GroupReference group;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the user which was assigned/unassgned from/to the group.")
	private UserReference user;

	public GroupUserAssignModel() {
	}

	public GroupReference getGroup() {
		return group;
	}

	public void setGroup(GroupReference group) {
		this.group = group;
	}

	public UserReference getUser() {
		return user;
	}

	public void setUser(UserReference user) {
		this.user = user;
	}

}
