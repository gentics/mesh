package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;

/**
 * Assert for a schema container version
 */
public class SchemaContainerVersionAssert extends AbstractAssert<SchemaContainerVersionAssert, GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?>> {

	public SchemaContainerVersionAssert(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> actual) {
		super(actual, SchemaContainerVersionAssert.class);
	}

	/**
	 * Assert equality by comparing name and version of the container.
	 * 
	 * @param containerVersion
	 * @return
	 */
	public SchemaContainerVersionAssert equals(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> containerVersion) {
		assertThat(actual.getName()).as(descriptionText() + " Name").isEqualTo(containerVersion.getName());
		assertThat(actual.getVersion()).as(descriptionText() + " Version").isEqualTo(containerVersion.getVersion());
		return this;
	}
}
