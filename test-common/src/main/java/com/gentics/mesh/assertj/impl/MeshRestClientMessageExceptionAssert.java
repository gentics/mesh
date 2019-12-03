package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.rest.client.MeshRestClientMessageException;

public class MeshRestClientMessageExceptionAssert extends AbstractAssert<MeshRestClientMessageExceptionAssert, MeshRestClientMessageException> {

	public MeshRestClientMessageExceptionAssert(MeshRestClientMessageException actual) {
		super(actual, MeshRestClientMessageExceptionAssert.class);
	}

	public MeshRestClientMessageExceptionAssert hasStatusCode(int code) {
		assertThat(actual.getStatusCode()).as("Has status code " + code).isEqualTo(code);
		return this;
	}

	public MeshRestClientMessageExceptionAssert hasMessage(String message) {
		assertThat(actual.getResponseMessage().getMessage()).isEqualTo(message);
		return this;
	}
}
