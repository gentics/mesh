package com.gentics.mesh.core.rest.group.request;

import org.codehaus.jackson.annotate.JsonIgnore;

public class GroupCreateRequest extends GroupUpdateRequest {

	@JsonIgnore
	private String uuid;

	public GroupCreateRequest() {
	}

}
