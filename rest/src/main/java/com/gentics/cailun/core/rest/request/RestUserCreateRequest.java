package com.gentics.cailun.core.rest.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestUserCreateRequest extends RestUserUpdateRequest {

	@JsonIgnore
	private String uuid;

	@JsonProperty(required = true)
	private String username;

	@JsonProperty(required = true)
	private String password;
}
