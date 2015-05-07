package com.gentics.mesh.core.rest.common.response;

public class GenericMessageResponse {

	private String message;

	public GenericMessageResponse() {
	}

	public GenericMessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
