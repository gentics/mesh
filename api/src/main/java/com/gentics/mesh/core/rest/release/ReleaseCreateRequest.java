package com.gentics.mesh.core.rest.release;

import com.gentics.mesh.core.rest.common.RestModel;

public class ReleaseCreateRequest implements RestModel {
	private String name;

	public ReleaseCreateRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
