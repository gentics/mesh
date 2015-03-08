package com.gentics.cailun.core.rest.user.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserCreateRequest extends UserUpdateRequest {

	@JsonIgnore
	private String uuid;

	@JsonProperty(required = true)
	private String username;

	@JsonProperty(required = true)
	private String password;
}
