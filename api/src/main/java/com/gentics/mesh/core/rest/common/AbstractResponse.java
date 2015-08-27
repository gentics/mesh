package com.gentics.mesh.core.rest.common;

public abstract class AbstractResponse implements RestResponse {

	private String uuid;

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
