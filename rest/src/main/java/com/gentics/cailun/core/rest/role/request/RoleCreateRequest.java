package com.gentics.cailun.core.rest.role.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RoleCreateRequest extends RoleUpdateRequest {

	@JsonIgnore
	private String uuid;

}
