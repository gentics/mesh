package com.gentics.mesh.core.rest.group;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GroupCreateRequest extends GroupUpdateRequest {

	@JsonIgnore
	private String uuid;

	public GroupCreateRequest() {
	}

}
