package com.gentics.cailun.core.rest.content.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContentCreateRequest extends ContentUpdateRequest {

	@JsonIgnore
	private String uuid;
	
	public ContentCreateRequest() {
	}

}
