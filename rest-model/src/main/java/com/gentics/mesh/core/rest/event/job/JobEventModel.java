package com.gentics.mesh.core.rest.event.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;

/**
 * Model for job specific events. 
 */
public class JobEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the referenced job.")
	private String uuid;

	public JobEventModel() {
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
