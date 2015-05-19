package com.gentics.mesh.core.rest.meshnode.response;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;

public class MeshNodeResponse extends AbstractPropertyContainerModel {

	private String parentNodeUuid;

	public MeshNodeResponse() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}
}
