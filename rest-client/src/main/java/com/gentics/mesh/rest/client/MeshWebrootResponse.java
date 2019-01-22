package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.node.NodeResponse;

public interface MeshWebrootResponse {

	/**
	 * Tests if the response is binary data.
	 * @return
	 */
	boolean isBinary();

	/**
	 * Gets the binary response or null if the response is not binary data
	 * @return
	 */
	MeshBinaryResponse getBinaryResponse();

	/**
	 * Gets the node response or null if the response is binary data
	 * @return
	 */
	NodeResponse getNodeResponse();
}
