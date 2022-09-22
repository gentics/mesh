package com.gentics.mesh.rest.client;

import java.io.Closeable;

import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * Definition of a WebRootResponse. The webroot response is special since it can return JSON of the {@link NodeResponse} or a {@link MeshBinaryResponse} of the
 * binary data of a binary field. This behaviour is controlled by the Accept header of the client request and the queried node (e.g. whether it uses a binary field for the segment).
 * @implNote It is important to close the response by calling {@link #close()} when the response is no longer needed. Failing to do so might lead to a connection leak.
 */
public interface MeshWebrootResponse extends Closeable {

	/**
	 * Tests if the response is binary data.
	 * 
	 * @return
	 */
	boolean isBinary();

	/**
	 * Gets the binary response or null if the response is not binary data
	 * 
	 * @return
	 */
	MeshBinaryResponse getBinaryResponse();

	/**
	 * Gets the node response or null if the response is binary data
	 * 
	 * @return
	 */
	NodeResponse getNodeResponse();

	/**
	 * Returns the node uuid header value of the loaded content.
	 * 
	 * @return
	 */
	String getNodeUuid();

	@Override
	void close();
}
