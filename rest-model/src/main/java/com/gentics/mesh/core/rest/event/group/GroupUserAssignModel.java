package com.gentics.mesh.core.rest.event.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.UserReferenceModel;

/**
 * POJO for user<->group assignment events.
 */
public class GroupUserAssignModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the group.")
	private GroupReference group;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the user which was assigned/unassgned from/to the group.")
	private UserReferenceModel user;

	public GroupUserAssignModel() {
	}

	public GroupReference getGroup() {
		return group;
	}

	public void setGroup(GroupReference group) {
		this.group = group;
	}

	public UserReferenceModel getUser() {
		return user;
	}

	public void setUser(UserReferenceModel user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return String.format("%s, group: %s, user: %s", getEvent(), group, user);
	}
}
