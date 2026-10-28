package com.gentics.mesh.plugin.common.model;

import com.gentics.mesh.core.rest.common.AbstractResponse;

/**
 * A response containing only the UUID of the created node.
 */
public class NodeCreatedResponse extends AbstractResponse {

	public NodeCreatedResponse(String uuid) {
		setUuid(uuid);
	}
}
