package com.gentics.mesh.core.rest.tag.request;

import org.codehaus.jackson.annotate.JsonIgnore;

public class TagCreateRequest extends TagUpdateRequest {

	@JsonIgnore
	private String uuid;

	public TagCreateRequest() {
	}

}
