package com.gentics.mesh.core.rest.node;

/**
 * POJO for a node create request.
 */
public class NodeCreateRequest extends NodeUpdateRequest {

	private String parentNodeUuid;

	// TODO maybe we want to set the tagPath as well (alternative to tagUuid)

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
