package com.gentics.mesh.rest.client;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Exception to be used for JSON errors in the REST client.
 */
public class MeshRestClientJsonObjectException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private ObjectNode responseInfo;

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage) {
		this(statusCode, statusMessage, null);
	}

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage, ObjectNode responseInfo) {
		super(statusMessage);
		this.statusCode = statusCode;
		this.responseInfo = responseInfo;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public ObjectNode getResponseInfo() {
		return responseInfo;
	}

}
