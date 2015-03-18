package com.gentics.cailun.core.rest.group.request;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class GroupUpdateRequest extends AbstractRestModel {

	private String name;

	public GroupUpdateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
