package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.branch.BranchResponse;

public class BranchResponseAssert extends AbstractAssert<BranchResponseAssert, BranchResponse> {
	public BranchResponseAssert(BranchResponse actual) {
		super(actual, BranchResponseAssert.class);
	}

	/**
	 * Assert that the branch response has the given name.
	 * 
	 * @param name
	 * @return fluent API
	 */
	public BranchResponseAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that the branch response has the given uuid.
	 * 
	 * @param uuid
	 * @return fluent API
	 */
	public BranchResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as(descriptionText() + " uuid").isEqualTo(uuid);
		return this;
	}

	/**
	 * Assert that the branch response has the given hostname.
	 * 
	 * @param hostname
	 * @return fluent API
	 */
	public BranchResponseAssert hasHostname(String hostname) {
		assertThat(actual.getHostname()).as(descriptionText() + " hostname").isEqualTo(hostname);
		return this;
	}

	/**
	 * Assert that the branch response has the given ssl flag.
	 * 
	 * @param flag
	 * @return fluent API
	 */
	public BranchResponseAssert hasSSL(Object flag) {
		assertThat(actual.getSsl()).as(descriptionText() + " ssl").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert that the branch response has the given SSL setting.
	 * 
	 * @param ssl
	 * @return fluent API
	 */
	public BranchResponseAssert hasSsl(Boolean ssl) {
		assertThat(actual.getSsl()).as(descriptionText() + " SSL").isEqualTo(ssl);
		return this;
	}

	/**
	 * Assert that the branch response is marked active.
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isActive() {
		// TODO disabled since the feature is not yet fully implemented
		// assertThat(actual.isActive()).as(descriptionText() + " active").isTrue();
		return this;
	}

	/**
	 * Assert that the branch response is marked inactive.
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isInactive() {
		// TODO disabled since the feature is not yet fully implemented
		// assertThat(actual.isActive()).as(descriptionText() + " active").isFalse();
		return this;
	}

	/**
	 * Assert that all nodes have been migrated to this branch.
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isTrue();
		return this;
	}

	/**
	 * Assert that not all nodes have been migrated to this branch.
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isNotMigrated() {
		assertThat(actual.isMigrated()).as(descriptionText() + " migrated").isFalse();
		return this;
	}
}
