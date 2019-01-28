package com.gentics.mesh.rest.client;

import org.codehaus.jettison.json.JSONObject;

public class MeshRestClientJsonObjectException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private JSONObject responseInfo;

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage) {
		this(statusCode, statusMessage, null);
	}

	public MeshRestClientJsonObjectException(int statusCode, String statusMessage, JSONObject responseInfo) {
		super(statusMessage);
		this.statusCode = statusCode;
		this.responseInfo = responseInfo;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public JSONObject getResponseInfo() {
		return responseInfo;
	}

}
