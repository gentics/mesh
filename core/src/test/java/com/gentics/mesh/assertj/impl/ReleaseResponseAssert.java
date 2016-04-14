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

	/**
	 * Assert that the release response has the given uuid
	 * @param uuid
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as(descriptionText() + " uuid").isEqualTo(uuid);
		return this;
	}

	/**
	 * Assert that the release response is marked active
	 * @return fluent API
	 */
	public ReleaseResponseAssert isActive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isTrue();
		return this;
	}

	/**
	 * Assert that the release response is marked inactive
	 * @return fluent API
	 */
	public ReleaseResponseAssert isInactive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isFalse();
		return this;
	}

	/**
	 * Assert that all nodes have been migrated to this release
	 * @return fluent API
	 */
	public ReleaseResponseAssert isMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isTrue();
		return this;
	}

	/**
	 * Assert that not all nodes have been migrated to this release
	 * @return fluent API
	 */
	public ReleaseResponseAssert isNotMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isFalse();
		return this;
	}
}
