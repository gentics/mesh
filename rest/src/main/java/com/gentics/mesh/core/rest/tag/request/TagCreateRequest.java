package com.gentics.mesh.core.rest.tag.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TagCreateRequest extends TagUpdateRequest {

	@JsonIgnore
	private String uuid;

	public TagCreateRequest() {
	}

}
