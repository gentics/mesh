package com.gentics.mesh.core.rest.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.common.RestModel;

public class MeshStatusResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The current Gentics Mesh server status.")
	private MeshStatus status;

	public MeshStatus getStatus() {
		return status;
	}

	public MeshStatusResponse setStatus(MeshStatus status) {
		this.status = status;
		return this;
	}

}
