package com.gentics.mesh.rest.client;

import io.vertx.core.json.JsonObject;

public class MeshRestClientJsonObjectException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private JsonObject responseInfo;

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage) {
		this(statusCode, statusMessage, null);
	}

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage, JsonObject responseInfo) {
		super(statusMessage);
		this.statusCode = statusCode;
		this.responseInfo = responseInfo;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public JsonObject getResponseInfo() {
		return responseInfo;
	}

}
