package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

public class MeshRestClientHttpException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private GenericMessageResponse responseMessage;

	public MeshRestClientHttpException(int statusCode, String statusMessage, GenericMessageResponse responseMessage) {
		super(statusMessage);
		this.statusCode = statusCode;
		this.responseMessage = responseMessage;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public GenericMessageResponse getResponseMessage() {
		return responseMessage;
	}

}
