package com.gentics.mesh.rest.client.impl;

public final class EmptyResponse {
	private EmptyResponse() {

	}

	private static final EmptyResponse instance = new EmptyResponse();

	public static EmptyResponse getInstance() {
		return instance;
	}
}
