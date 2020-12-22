package com.gentics.mesh.rest.client.impl;

/**
 * Empty response object to be used for code 204 responses.
 */
public final class EmptyResponse {

	private EmptyResponse() {

	}

	private static final EmptyResponse instance = new EmptyResponse();

	public static EmptyResponse getInstance() {
		return instance;
	}
}
