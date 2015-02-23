package com.gentics.cailun.core.rest.response;

public class GenericNotFoundResponse {

	private String message;

	public GenericNotFoundResponse() {
	}

	public GenericNotFoundResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
