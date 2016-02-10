package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.schema.SchemaContainer;

/**
 * Assert for a schema container
 */
public class SchemaContainerAssert extends AbstractAssert<SchemaContainerAssert, SchemaContainer> {

	public SchemaContainerAssert(SchemaContainer actual) {
		super(actual, SchemaContainerAssert.class);
	}

	/**
	 * Assert equality by comparing name and version of the container.
	 * 
	 * @param container
	 * @return
	 */
	public SchemaContainerAssert equals(SchemaContainer container) {
		assertThat(actual.getName()).as(descriptionText() + " Name").isEqualTo(container.getName());
		assertThat(actual.getVersion()).as(descriptionText() + " Version").isEqualTo(container.getVersion());
		return this;
	}
}
