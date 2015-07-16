package com.gentics.mesh.core.rest.role;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
