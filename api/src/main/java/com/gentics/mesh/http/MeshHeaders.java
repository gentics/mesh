package com.gentics.mesh.http;

/**
 * Collection of Gentics Mesh specific http headers.
 */
public final class MeshHeaders {

	/**
	 * Header which can be used to influence the anonymous access handling.
	 */
	public static final String ANONYMOUS_AUTHENTICATION = "Anonymous-Authentication";

	/**
	 * Header which will be set by the webroot endpoint. The value will indicate whether the response is either a node or a binary response.
	 */
	public static final String WEBROOT_RESPONSE_TYPE = "Webroot-Response-Type";

	/**
	 * Header which will be set by the webroot endpoint. The value of the response identifies the node of the found content.
	 */
	public static final String WEBROOT_NODE_UUID = "Webroot-Node-Uuid";
}
