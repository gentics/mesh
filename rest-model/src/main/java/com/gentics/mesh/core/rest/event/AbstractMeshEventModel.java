package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public abstract class AbstractMeshEventModel implements MeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the referenced element.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the referenced element.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the mesh node from which the event originates.")
	private String origin;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Some events will be caused by another action. This object contains information about the cause of the event.")
	private EventCauseInfo cause;

	@JsonIgnore
	private String address;

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public EventCauseInfo getCause() {
		return cause;
	}

	@Override
	public void setCause(EventCauseInfo cause) {
		this.cause = cause;
	}

}
