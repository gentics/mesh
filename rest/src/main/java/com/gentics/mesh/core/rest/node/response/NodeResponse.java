package com.gentics.mesh.core.rest.node.response;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;

public class NodeResponse extends AbstractPropertyContainerModel {

	private String parentNodeUuid;

	public NodeResponse() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}
}
