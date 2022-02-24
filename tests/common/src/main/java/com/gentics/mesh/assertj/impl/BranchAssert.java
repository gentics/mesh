package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;

public class BranchAssert extends AbstractAssert<BranchAssert, HibBranch> {
	public BranchAssert(HibBranch actual) {
		super(actual, BranchAssert.class);
	}

	/**
	 * Assert that the branch is active.
	 * 
	 * @return fluent API
	 */
	public BranchAssert isActive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isTrue();
		return this;
	}

	/**
	 * Assert that the branch is not active.
	 * 
	 * @return fluent API
	 */
	public BranchAssert isInactive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isFalse();
		return this;
	}

	/**
	 * Assert that the branch has the given name.
	 * 
	 * @param name
	 *            name
	 * @return fluent API
	 */
	public BranchAssert isNamed(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that the branch has a not empty uuid.
	 * 
	 * @return fluent API
	 */
	public BranchAssert hasUuid() {
		assertThat(actual.getUuid()).as(descriptionText() + " uuid").isNotEmpty();
		return this;
	}

	/**
	 * Assert that the branch matches the given one
	 * 
	 * @param branch
	 *            branch to match
	 * @return fluent API
	 */
	public BranchAssert matches(HibBranch branch) {
		if (branch == null) {
			assertThat(actual).as(descriptionText()).isNull();
		} else {
			assertThat(actual).as(descriptionText()).isNotNull();
			assertThat(actual.getUuid()).as(descriptionText() + " uuid").isEqualTo(branch.getUuid());
			assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(branch.getName());
		}
		return this;
	}

	/**
	 * Assert that the next branch matches the given one.
	 * 
	 * @param branch
	 *            branch to match
	 * @return fluent API
	 */
	public BranchAssert hasNext(HibBranch branch) {
		assertThat(actual.getNextBranches().get(0)).as(descriptionText() + " next branch").matches(branch);
		return this;
	}

	/**
	 * Assert that the previous branch matches the given one.
	 * 
	 * @param branch
	 *            branch to match
	 * @return fluent API
	 */
	public BranchAssert hasPrevious(HibBranch branch) {
		assertThat(actual.getPreviousBranch()).as(descriptionText() + " previous branch").matches(branch);
		return this;
	}

	/**
	 * Assert that the schema version is assigned to the branch.
	 * 
	 * @param version
	 *            schema version
	 * @return fluent API
	 */
	public BranchAssert hasSchemaVersion(HibSchemaVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isTrue();
		return this;
	}

	/**
	 * Assert that the schema version is not assigned to the branch.
	 * 
	 * @param version
	 *            schema version
	 * @return fluent API
	 */
	public BranchAssert hasNotSchemaVersion(HibSchemaVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isFalse();
		return this;
	}

	/**
	 * Assert that the schema is assigned (in any version) to the branch.
	 * 
	 * @param schemaContainer
	 *            schema
	 * @return fluent API
	 */
	public BranchAssert hasSchema(HibSchema schemaContainer) {
		assertThat(actual.contains(schemaContainer)).as(descriptionText() + " has schema").isTrue();
		return this;
	}

	/**
	 * Assert that the schema is not assigned (in any version) to the branch.
	 * 
	 * @param schemaContainer
	 *            schema
	 * @return fluent API
	 */
	public BranchAssert hasNotSchema(HibSchema schemaContainer) {
		assertThat(actual.contains(schemaContainer)).as(descriptionText() + " has schema").isFalse();
		return this;
	}

	/**
	 * Assert that the microschema version is assigned to the branch.
	 * 
	 * @param version
	 *            microschema version
	 * @return fluent API
	 */
	public BranchAssert hasMicroschemaVersion(HibMicroschemaVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isTrue();
		return this;
	}

	/**
	 * Assert that the microschema version is not assigned to the branch.
	 * 
	 * @param version
	 *            schema version
	 * @return fluent API
	 */
	public BranchAssert hasNotMicroschemaVersion(HibMicroschemaVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isFalse();
		return this;
	}

	/**
	 * Assert that the microschema is assigned (in any version) to the branch.
	 * 
	 * @param microschema
	 *            microschema
	 * @return fluent API
	 */
	public BranchAssert hasMicroschema(HibMicroschema microschema) {
		assertThat(actual.contains(microschema)).as(descriptionText() + " has schema").isTrue();
		return this;
	}

	/**
	 * Assert that the microschema is not assigned (in any version) to the branch.
	 * 
	 * @param microschema
	 *            microschema
	 * @return fluent API
	 */
	public BranchAssert hasNotMicroschema(HibMicroschema microschema) {
		assertThat(actual.contains(microschema)).as(descriptionText() + " has schema").isFalse();
		return this;
	}

	/**
	 * Assert that the branch is tagged with the given tags (possibly among others)
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchAssert isTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(HibTag::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").contains(tags);
		return this;
	}

	/**
	 * Assert that the branch is not tagged with any of the given tags
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchAssert isNotTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(HibTag::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").doesNotContain(tags);
		return this;
	}

	/**
	 * Assert that the branch is only tagged with the given tags
	 * @param tags tag names
	 * @return fluent API
	 */
	public BranchAssert isOnlyTagged(String... tags) {
		Set<String> tagNames = actual.getTags().stream().map(HibTag::getName).collect(Collectors.toSet());
		assertThat(tagNames).as(descriptionText() + " tags").containsOnly(tags);
		return this;
	}
}
