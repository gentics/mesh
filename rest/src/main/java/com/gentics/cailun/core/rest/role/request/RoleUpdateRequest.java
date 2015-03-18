package com.gentics.cailun.core.rest.role.request;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class RoleUpdateRequest extends AbstractRestModel {

	private String name;

	public RoleUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
