package com.gentics.mesh.core.rest.common;

public abstract class AbstractResponse implements RestModel {

	private String uuid;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
