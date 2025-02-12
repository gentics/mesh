package com.gentics.mesh.rest.client;

/**
 * A switch between different HTTP versions to use in Mesh REST connection.
 * 
 * @author plyhun
 *
 */
public enum ProtocolVersion {
	/**
	 * Use any protocol supported by the carrier HTTP client library. 
	 * The connection may be escalated from HTTP/1.x to HTTP/2.
	 */
	DEFAULT,
	/**
	 * Limit to HTTP/1.1.
	 */
	HTTP_1_1,
	/**
	 * Limit to HTTP/2.
	 */
	HTTP_2
}
