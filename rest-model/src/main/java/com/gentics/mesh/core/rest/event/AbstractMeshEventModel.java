package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;

public abstract class AbstractMeshEventModel implements MeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the mesh node from which the event originates.")
	private String origin;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Some events will be caused by another action. This object contains information about the cause of the event.")
	private EventCauseInfo cause;

	@JsonIgnore
	private MeshEvent event;

	public AbstractMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event) {
		this.origin = origin;
		this.cause = cause;
		this.event = event;
	}

	@Override
	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	@Override
	public MeshEvent getEvent() {
		return event;
	}

	public void setEvent(MeshEvent event) {
		this.event = event;
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
