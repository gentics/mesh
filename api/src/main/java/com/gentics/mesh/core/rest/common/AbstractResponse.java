package com.gentics.mesh.core.rest.common;

/**
 * Abstract response which provides the uuid.
 */
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

	@Override
	public String toString() {
		return getClass().getSimpleName() + " Uuid: " + getUuid();
	}

}
