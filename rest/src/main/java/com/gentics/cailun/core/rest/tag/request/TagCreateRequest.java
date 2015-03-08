package com.gentics.cailun.core.rest.tag.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TagCreateRequest extends TagUpdateRequest {

	@JsonIgnore
	private String uuid;

	public TagCreateRequest() {
	}
}
