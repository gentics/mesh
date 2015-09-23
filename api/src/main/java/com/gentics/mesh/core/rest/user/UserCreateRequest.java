package com.gentics.mesh.core.rest.user;

public class UserCreateRequest extends UserUpdateRequest {

	/**
	 * UUID of the parent group for this user.
	 */
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
