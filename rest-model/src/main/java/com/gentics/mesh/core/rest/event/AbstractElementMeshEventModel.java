package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public abstract class AbstractElementMeshEventModel extends AbstractMeshEventModel implements MeshElementEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the referenced element.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the referenced element.")
	private String name;

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
