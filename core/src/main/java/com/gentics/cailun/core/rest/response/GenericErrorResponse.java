package com.gentics.cailun.core.rest.response;

public class GenericErrorResponse {

	private String message;

	public GenericErrorResponse() {
	}

	public GenericErrorResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
