package com.gentics.mesh.core.rest.role;

/**
 * Role request model that is used for role creation.
 */
public class RoleCreateRequest extends RoleUpdateRequest {

	private String groupUuid;

	public RoleCreateRequest() {
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
	}

}
