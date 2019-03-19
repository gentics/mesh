package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;

public abstract class AbstractElementMeshEventModel extends AbstractMeshEventModel implements MeshElementEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the referenced element.")
	protected String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the referenced element.")
	protected String name;

	public AbstractElementMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, String uuid, String name) {
		super(origin, cause, event);
		this.uuid = uuid;
		this.name = name;
	}

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

}
