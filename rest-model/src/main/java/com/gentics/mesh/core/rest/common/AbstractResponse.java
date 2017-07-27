package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Abstract response which provides the uuid.
 */
public abstract class AbstractResponse implements RestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the element")
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
