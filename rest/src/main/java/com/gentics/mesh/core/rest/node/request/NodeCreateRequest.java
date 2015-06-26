package com.gentics.mesh.core.rest.node.request;

import org.codehaus.jackson.annotate.JsonIgnore;

public class NodeCreateRequest extends NodeUpdateRequest {

	@JsonIgnore
	private String uuid;

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
