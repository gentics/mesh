package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.PublishStatusModel;

public class PublishStatusModelAssert extends AbstractAssert<PublishStatusModelAssert, PublishStatusModel> {

	public PublishStatusModelAssert(PublishStatusModel actual) {
		super(actual, PublishStatusModelAssert.class);
	}

	/**
	 * Assert that the status is published
	 * @return fluent API
	 */
	public PublishStatusModelAssert isPublished() {
		assertThat(actual.isPublished()).isTrue();
		return this;
	}

	/**
	 * Assert that the status is not published
	 * @return fluent API
	 */
	public PublishStatusModelAssert isNotPublished() {
		assertThat(actual.isPublished()).isFalse();
		return this;
	}

	/**
	 * Assert that the status references the given version
	 * @param version
	 * @return fluent API
	 */
	public PublishStatusModelAssert hasVersion(String version) {
		assertThat(actual.getVersion()).as(descriptionText() + " Version").isNotNull();
		assertThat(actual.getVersion().getNumber()).as(descriptionText() + " Version").isEqualTo(version);
		return this;
	}
}
