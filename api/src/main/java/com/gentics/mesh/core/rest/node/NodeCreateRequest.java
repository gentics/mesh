package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * POJO for a node create request.
 */
public class NodeCreateRequest extends NodeUpdateRequest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the parent node in which the node will be created")
	private String parentNodeUuid;

	public NodeCreateRequest() {
	}

	/**
	 * Return the parent node uuid.
	 * 
	 * @return
	 */
	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	/**
	 * Set the parent node uuid for the node that should be created.
	 * 
	 * @param parentNodeUuid
	 */
	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

}
