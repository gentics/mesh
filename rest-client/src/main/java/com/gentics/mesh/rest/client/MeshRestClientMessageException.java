package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

/**
 * Rest client exception which stores the error information in within a generic message response.
 */
public class MeshRestClientMessageException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private GenericMessageResponse responseMessage;

	public MeshRestClientMessageException(int statusCode, String statusMessage) {
		this(statusCode, statusMessage, null);
	}

	public MeshRestClientMessageException(int statusCode, String statusMessage, GenericMessageResponse responseMessage) {
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

	@Override
	public String getMessage() {
		if (responseMessage == null) {
			return super.getMessage();
		}
		return responseMessage.getMessage();
	}

}
