package com.gentics.mesh.core.rest.event.group;

import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.UserReference;

public class GroupUserAssignModel extends AbstractMeshEventModel {

	private GroupReference group;
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
