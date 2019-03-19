package com.gentics.mesh.core.rest.event.group;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.UserReference;

public class GroupUserAssignModel extends AbstractMeshEventModel {

	private GroupReference group;
	private UserReference user;

	@JsonCreator
	public GroupUserAssignModel(String origin, EventCauseInfo cause, MeshEvent event) {
		super(origin, cause, event);
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
