package com.gentics.mesh.core.rest.admin.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO which represents the mesh server status.
 */
public class MeshStatusResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The current Gentics Mesh server status.")
	private MeshStatus status;

	public MeshStatus getStatus() {
		return status;
	}

	@Setter
	public MeshStatusResponse setStatus(MeshStatus status) {
		this.status = status;
		return this;
	}

}
