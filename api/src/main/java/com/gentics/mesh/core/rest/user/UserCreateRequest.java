package com.gentics.mesh.core.rest.user;


public class UserCreateRequest extends UserUpdateRequest {

	/**
	 * UUID of the parent group for this user.
	 */
	private String groupUuid;

	public UserCreateRequest() {
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
	}

}
