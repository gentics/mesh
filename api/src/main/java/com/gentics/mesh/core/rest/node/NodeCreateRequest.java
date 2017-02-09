package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.user.NodeReference;

/**
 * POJO for a node create request.
 */
public class NodeCreateRequest extends NodeUpdateRequest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the parent node in which the node will be created. The uuid of this object must be set.")
	private NodeReference parentNode;

	public NodeCreateRequest() {
	}

	/**
	 * Return the parent node reference.
	 * 
	 * @return
	 */
	public NodeReference getParentNode() {
		return parentNode;
	}

	/**
	 * Set the parent node reference for the node that should be created.
	 * 
	 * @param parentNode
	 * @return Fluent API
	 */
	public NodeCreateRequest setParentNode(NodeReference parentNode) {
		this.parentNode = parentNode;
		return this;
	}

	/**
	 * Helper method which can be used to quickly set the parent node uuid.
	 * 
	 * @param uuid
	 */
	public void setParentNodeUuid(String uuid) {
		this.parentNode = new NodeReference().setUuid(uuid);
	}

}
