package com.gentics.mesh.core.rest.group.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GroupCreateRequest extends GroupUpdateRequest {
	@JsonIgnore
	private String uuid;

	public GroupCreateRequest() {
	}

}
