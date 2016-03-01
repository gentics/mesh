package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.release.ReleaseResponse;

public class ReleaseResponseAssert extends AbstractAssert<ReleaseResponseAssert, ReleaseResponse> {
	public ReleaseResponseAssert(ReleaseResponse actual) {
		super(actual, ReleaseResponseAssert.class);
	}

	/**
	 * Assert that the release response has the given name
	 * @param name
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}
}
