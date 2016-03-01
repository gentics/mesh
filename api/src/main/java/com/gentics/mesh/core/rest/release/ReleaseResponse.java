package com.gentics.mesh.core.rest.release;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class ReleaseResponse extends AbstractGenericRestResponse {
	private String name;

	public ReleaseResponse() {
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
