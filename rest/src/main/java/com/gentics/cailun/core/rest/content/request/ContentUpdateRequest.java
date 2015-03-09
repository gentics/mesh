package com.gentics.cailun.core.rest.content.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public class ContentUpdateRequest extends ContentResponse {

	@JsonIgnore
	private UserResponse author;

}
