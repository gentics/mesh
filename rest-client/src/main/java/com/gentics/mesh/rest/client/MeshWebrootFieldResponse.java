package com.gentics.mesh.rest.client;

import java.io.Closeable;

/**
 * Response for {@link WebRootFieldClientMethods}
 * 
 * @author plyhun
 *
 * @implNote It is important to close the response by calling {@link #close()} when the response is no longer needed. Failing to do so might lead to a connection leak.
 */
public interface MeshWebrootFieldResponse extends Closeable {

	/**
	 * Tests if the response is binary data.
	 * 
	 * @return
	 */
	boolean isBinary();

	/**
	 * Tests if the response is redirected(302)
	 *
	 * @return
	 */
	boolean isRedirected();
	
	/**
	 * Tests if the response is plain text data, i.e. not a binary, not a JSON structure/array.
	 * 
	 * @return
	 */
	boolean isPlainText();
	
	/**
	 * Get field type name. 
	 * 
	 * @return
	 */
	String getFieldType();

	/**
	 * Gets the binary response or null if the response is not binary data
	 * 
	 * @return
	 */
	MeshBinaryResponse getBinaryResponse();

	/**
	 * Returns the node uuid header value of the loaded content.
	 * 
	 * @return
	 */
	String getNodeUuid();
	
	/**
	 * Get response as a JSON string. May be null for the binary content, or JSON value otherwise.
	 * 
	 * @return
	 */
	String getResponseAsJsonString();

	/**
	 * Get response as a plain text string. May be null if content is binary or JSON structure.
	 * 
	 * @return
	 */
	String getResponseAsPlainText();

	@Override
	void close();
}
