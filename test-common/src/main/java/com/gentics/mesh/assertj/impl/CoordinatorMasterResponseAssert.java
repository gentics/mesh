package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;

public class CoordinatorMasterResponseAssert extends AbstractAssert<CoordinatorMasterResponseAssert, CoordinatorMasterResponse> {
	public CoordinatorMasterResponseAssert(CoordinatorMasterResponse actual) {
		super(actual, CoordinatorMasterResponseAssert.class);
	}

	public CoordinatorMasterResponseAssert hasName(String expected) {
		assertThat(actual.getName()).isEqualTo(expected);
		return this;
	}

	public CoordinatorMasterResponseAssert hasHost(String expected) {
		assertThat(actual.getHost()).isEqualTo(expected);
		return this;
	}

	public CoordinatorMasterResponseAssert hasPort(int expected) {
		assertThat(actual.getPort()).isEqualTo(expected);
		return this;
	}
}
