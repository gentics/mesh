package com.gentics.mesh.core.rest.user.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserCreateRequest extends UserUpdateRequest {

	@JsonIgnore
	private String uuid;

	/**
	 * UUID of the parent group for this user.
	 */
	private String groupUuid;

	@JsonProperty(required = true)
	private String username;

	@JsonProperty(required = true)
	private String password;
	
	public UserCreateRequest() {
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
	}
}
