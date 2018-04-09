package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.release.ReleaseResponse;

public class ReleaseResponseAssert extends AbstractAssert<ReleaseResponseAssert, ReleaseResponse> {
	public ReleaseResponseAssert(ReleaseResponse actual) {
		super(actual, ReleaseResponseAssert.class);
	}

	/**
	 * Assert that the release response has the given name.
	 * 
	 * @param name
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that the release response has the given uuid.
	 * 
	 * @param uuid
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as(descriptionText() + " uuid").isEqualTo(uuid);
		return this;
	}

	/**
	 * Assert that the release response has the given hostname.
	 * 
	 * @param hostname
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasHostname(String hostname) {
		assertThat(actual.getHostname()).as(descriptionText() + " hostname").isEqualTo(hostname);
		return this;
	}

	/**
	 * Assert that the release response has the given ssl flag.
	 * 
	 * @param flag
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasSSL(Object flag) {
		assertThat(actual.getSsl()).as(descriptionText() + " ssl").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert that the release response has the given SSL setting.
	 * 
	 * @param ssl
	 * @return fluent API
	 */
	public ReleaseResponseAssert hasSsl(Boolean ssl) {
		assertThat(actual.getSsl()).as(descriptionText() + " SSL").isEqualTo(ssl);
		return this;
	}

	/**
	 * Assert that the release response is marked active.
	 * 
	 * @return fluent API
	 */
	public ReleaseResponseAssert isActive() {
		// TODO disabled since the feature is not yet fully implemented
		// assertThat(actual.isActive()).as(descriptionText() + " active").isTrue();
		return this;
	}

	/**
	 * Assert that the release response is marked inactive.
	 * 
	 * @return fluent API
	 */
	public ReleaseResponseAssert isInactive() {
		// TODO disabled since the feature is not yet fully implemented
		// assertThat(actual.isActive()).as(descriptionText() + " active").isFalse();
		return this;
	}

	/**
	 * Assert that all nodes have been migrated to this release.
	 * 
	 * @return fluent API
	 */
	public ReleaseResponseAssert isMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isTrue();
		return this;
	}

	/**
	 * Assert that not all nodes have been migrated to this release.
	 * 
	 * @return fluent API
	 */
	public ReleaseResponseAssert isNotMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isFalse();
		return this;
	}
}
