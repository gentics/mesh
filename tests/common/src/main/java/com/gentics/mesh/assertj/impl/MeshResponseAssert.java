package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.distributed.RequestDelegator;
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

	public MeshResponseAssert isForwardedFrom(String nodeName) {
		Optional<String> header = actual.getHeader(RequestDelegator.MESH_FORWARDED_FROM_HEADER);
		assertThat(header)
			.withFailMessage("Expecting request to be forwarded from \"%s\" but it was not forwarded at all.", nodeName)
			.isPresent();
		hasHeader(RequestDelegator.MESH_FORWARDED_FROM_HEADER, nodeName)
			.withFailMessage("Expecting request to be forwarded from \"%s\" but it was forwarded from \"%s\".", nodeName, header.get());
		return this;
	}

	public MeshResponseAssert isNotForwarded() {
		Optional<String> header = actual.getHeader(RequestDelegator.MESH_FORWARDED_FROM_HEADER);
		assertThat(header)
			.withFailMessage("Expecting request not to be forwarded but it was forwarded from \"%s\".", header.orElse(null))
			.isEmpty();
		return this;
	}
}
