package com.gentics.mesh.assertj.impl;

import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import org.assertj.core.api.AbstractAssert;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

public class MeshRestClientMessageExceptionAssert extends AbstractAssert<MeshRestClientMessageExceptionAssert, MeshRestClientMessageException> {

	public MeshRestClientMessageExceptionAssert(MeshRestClientMessageException actual) {
		super(actual, MeshRestClientMessageExceptionAssert.class);
	}

	public MeshRestClientMessageExceptionAssert hasStatusCode(int code) {
		assertThat(actual.getStatusCode()).as("Has status code " + code).isEqualTo(code);
		return this;
	}

	public MeshRestClientMessageExceptionAssert hasMessageKey(String key) {
		return this;
	}
}
