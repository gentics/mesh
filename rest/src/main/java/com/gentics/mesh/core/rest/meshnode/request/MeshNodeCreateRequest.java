package com.gentics.mesh.core.rest.meshnode.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MeshNodeCreateRequest extends MeshNodeUpdateRequest {

	@JsonIgnore
	private String uuid;

	private String parentNodeUuid;

	// TODO maybe we want to set the tagPath as well (alternative to tagUuid)

	public MeshNodeCreateRequest() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

}
