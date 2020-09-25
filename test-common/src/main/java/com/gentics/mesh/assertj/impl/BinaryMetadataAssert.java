package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;

public class BinaryMetadataAssert extends AbstractAssert<BinaryMetadataAssert, BinaryMetadata> {
	public BinaryMetadataAssert(BinaryMetadata actual) {
		super(actual, BinaryMetadataAssert.class);
	}

	public BinaryMetadataAssert isEmpty() {
		assertThat(actual.getMap()).isEmpty();
		assertThat(actual.getLocation()).isNull();
		return this;
	}
}
