package com.gentics.mesh.core.rest.tag;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TagFamilyCreateRequest extends TagFamilyUpdateRequest {

	@JsonIgnore
	private String uuid;

	public TagFamilyCreateRequest() {
	}

}
