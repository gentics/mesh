package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractResponse that = (AbstractResponse) o;
		return Objects.equals(getUuid(), that.getUuid());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUuid());
	}
}
