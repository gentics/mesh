package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.hazelcast.instance.Node;

public class ReleaseAssert extends AbstractAssert<ReleaseAssert, Release> {
	public ReleaseAssert(Release actual) {
		super(actual, ReleaseAssert.class);
	}

	/**
	 * Assert that the release is active
	 * @return fluent API
	 */
	public ReleaseAssert isActive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isTrue();
		return this;
	}

	/**
	 * Assert that the release is not active
	 * @return fluent API
	 */
	public ReleaseAssert isInactive() {
		assertThat(actual.isActive()).as(descriptionText() + " active").isFalse();
		return this;
	}

	/**
	 * Assert that the release has the given name
	 * @param name name
	 * @return fluent API
	 */
	public ReleaseAssert isNamed(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that the release has a not empty uuid
	 * @return fluent API
	 */
	public ReleaseAssert hasUuid() {
		assertThat(actual.getUuid()).as(descriptionText() + " uuid").isNotEmpty();
		return this;
	}

	/**
	 * Assert that the release matches the given one
	 * @param release release to match
	 * @return fluent API
	 */
	public ReleaseAssert matches(Release release) {
		if (release == null) {
			assertThat(actual).as(descriptionText()).isNull();
		} else {
			assertThat(actual).as(descriptionText()).isNotNull();
			assertThat(actual.getUuid()).as(descriptionText() + " uuid").isEqualTo(release.getUuid());
			assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(release.getName());
		}
		return this;
	}

	/**
	 * Assert that the next release matches the given one
	 * @param release release to match
	 * @return fluent API
	 */
	public ReleaseAssert hasNext(Release release) {
		assertThat(actual.getNextRelease()).as(descriptionText() + " next release").matches(release);
		return this;
	}

	/**
	 * Assert that the previous release matches the given one
	 * @param release release to match
	 * @return fluent API
	 */
	public ReleaseAssert hasPrevious(Release release) {
		assertThat(actual.getPreviousRelease()).as(descriptionText() + " previous release").matches(release);
		return this;
	}

	/**
	 * Assert that the schema version is assigned to the release
	 * @param version schema version
	 * @return fluent API
	 */
	public ReleaseAssert hasSchemaVersion(SchemaContainerVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isTrue();
		return this;
	}

	/**
	 * Assert that the schema version is not assigned to the release
	 * @param version schema version
	 * @return fluent API
	 */
	public ReleaseAssert hasNotSchemaVersion(SchemaContainerVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isFalse();
		return this;
	}

	/**
	 * Assert that the schema is assigned (in any version) to the release
	 * @param schemaContainer schema
	 * @return fluent API
	 */
	public ReleaseAssert hasSchema(SchemaContainer schemaContainer) {
		assertThat(actual.contains(schemaContainer)).as(descriptionText() + " has schema").isTrue();
		return this;
	}

	/**
	 * Assert that the schema is not assigned (in any version) to the release
	 * @param schemaContainer schema
	 * @return fluent API
	 */
	public ReleaseAssert hasNotSchema(SchemaContainer schemaContainer) {
		assertThat(actual.contains(schemaContainer)).as(descriptionText() + " has schema").isFalse();
		return this;
	}

	/**
	 * Assert that the microschema version is assigned to the release
	 * @param version microschema version
	 * @return fluent API
	 */
	public ReleaseAssert hasMicroschemaVersion(MicroschemaContainerVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isTrue();
		return this;
	}

	/**
	 * Assert that the microschema version is not assigned to the release
	 * @param version schema version
	 * @return fluent API
	 */
	public ReleaseAssert hasNotMicroschemaVersion(MicroschemaContainerVersion version) {
		assertThat(actual.contains(version)).as(descriptionText() + " has version").isFalse();
		return this;
	}

	/**
	 * Assert that the microschema is assigned (in any version) to the release
	 * @param microschemaContainer microschema
	 * @return fluent API
	 */
	public ReleaseAssert hasMicroschema(MicroschemaContainer microschemaContainer) {
		assertThat(actual.contains(microschemaContainer)).as(descriptionText() + " has schema").isTrue();
		return this;
	}

	/**
	 * Assert that the microschema is not assigned (in any version) to the release
	 * @param microschemaContainer microschema
	 * @return fluent API
	 */
	public ReleaseAssert hasNotMicroschema(MicroschemaContainer microschemaContainer) {
		assertThat(actual.contains(microschemaContainer)).as(descriptionText() + " has schema").isFalse();
		return this;
	}
}
