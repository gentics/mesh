package com.gentics.mesh.core.rest.role.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RoleCreateRequest extends RoleUpdateRequest {

	@JsonIgnore
	private String uuid;

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
