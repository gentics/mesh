package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * POJO for a user create request model.
 */
public class UserCreateRequest extends UserUpdateRequest {

	@JsonPropertyDescription("Optional group id for the user. If provided the user will automatically be assigned to the identified group.")
	@JsonProperty(required = false)
	private String groupUuid;

	public UserCreateRequest() {
	}

	/**
	 * Return the group uuid for the group to which the user should be assigned.
	 * 
	 * @return
	 */
	public String getGroupUuid() {
		return groupUuid;
	}

	/**
	 * Set the group uuid for the group to which the user should be assigned.
	 * 
	 * @param groupUuid
	 */
	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
	}

}
