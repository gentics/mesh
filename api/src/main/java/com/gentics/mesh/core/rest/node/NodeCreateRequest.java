package com.gentics.mesh.core.rest.node;


public class NodeCreateRequest extends NodeUpdateRequest {

	private String parentNodeUuid;

	// TODO maybe we want to set the tagPath as well (alternative to tagUuid)

	public NodeCreateRequest() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

}
