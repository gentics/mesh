package com.gentics.cailun.core.rest.role.response;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class RoleResponse extends AbstractRestModel {

	private String name;
	
	//TODO add groups to response

	public RoleResponse() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
