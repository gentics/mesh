package com.gentics.mesh.core.rest.common;

public class GenericMessageResponse {

	private String message;

	private String internalMessage;

	public GenericMessageResponse() {
	}

	public GenericMessageResponse(String message) {
		this(message, null);
	}

	public GenericMessageResponse(String message, String internalMessage) {
		this.message = message;
		this.internalMessage = internalMessage;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getInternalMessage() {
		return internalMessage;
	}

	public void setInternalMessage(String internalMessage) {
		this.internalMessage = internalMessage;
	}
}
