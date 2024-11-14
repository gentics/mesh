package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.rest.client.MeshResponse;

public class MeshResponseAssert extends AbstractAssert<MeshResponseAssert, MeshResponse<?>> {
	public MeshResponseAssert(MeshResponse<?> actual) {
		super(actual, MeshResponseAssert.class);
	}

	public MeshResponseAssert hasHeader(String name, String value) {
		Optional<String> header = actual.getHeader(name);
		assertThat(header)
			.withFailMessage("Expecting header \"%s\" to be set to \"%s\" but it was not set at all.", name, value)
			.isPresent();
		assertThat(header)
			.withFailMessage("Expecting header \"%s\" to be set to \"%s\" but it was set to \"%s\".", name, value, header.get())
			.contains(value);
		return this;
	}
}
