package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.tag.TagReference;

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
	 * Assert that the branch response has the given path prefix.
	 * 
	 * @param prefix
	 * @return fluent API
	 */
	public BranchResponseAssert hasPathPrefix(String prefix) {
		assertThat(actual.getPathPrefix()).as(descriptionText() + " prefix").isEqualTo(prefix);
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
		assertThat(actual.getMigrated()).as(descriptionText() + " migrated").isTrue();
		return this;
	}

	/**
	 * Assert that not all nodes have been migrated to this branch.
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isNotMigrated() {
		assertThat(actual.getMigrated()).as(descriptionText() + " migrated").isFalse();
		return this;
	}

	/**
	 * Assert that this is the latest branch
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isLatest() {
		assertThat(actual.getLatest()).as(descriptionText() + " latest").isNotNull().isTrue();
		return this;
	}

	/**
	 * Assert that this is not the latest branch
	 * 
	 * @return fluent API
	 */
	public BranchResponseAssert isNotLatest() {
		assertThat(actual.getLatest()).as(descriptionText() + " latest").isNotNull().isFalse();
		return this;
	}

	/**
	 * Checks whether the given tag is listed within the branch rest response.
	 * 
	 * @param tag
	 * @return
	 */
	public boolean contains(Tag tag) {
		assertNotNull(tag);
		assertNotNull(tag.getUuid());
		assertNotNull(actual);
		assertThat(actual.getTags()).as(descriptionText() + " tags").isNotNull().isNotEmpty();

		for (TagReference tagRef : actual.getTags()) {
			if (tag.getUuid().equals(tagRef.getUuid())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Assert that the branch is tagged with the given tags (possibly among others)
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchResponseAssert isTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(TagReference::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").contains(tags);
		return this;
	}

	/**
	 * Assert that the branch is not tagged with any of the given tags
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchResponseAssert isNotTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(TagReference::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").doesNotContain(tags);
		return this;
	}

	/**
	 * Assert that the branch is only tagged with the given tags
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchResponseAssert isOnlyTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(TagReference::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").containsOnly(tags);
		return this;
	}
}
