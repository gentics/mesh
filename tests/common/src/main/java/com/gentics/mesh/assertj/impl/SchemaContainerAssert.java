package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;

/**
 * Assert for a schema container
 */
public class SchemaContainerAssert extends AbstractAssert<SchemaContainerAssert, GraphFieldSchemaContainer<?, ?, ?, ?>> {

	public SchemaContainerAssert(GraphFieldSchemaContainer<?, ?, ?, ?> actual) {
		super(actual, SchemaContainerAssert.class);
	}

	/**
	 * Assert equality by comparing name and version of the container.
	 * 
	 * @param container
	 * @return
	 */
	public SchemaContainerAssert equals(GraphFieldSchemaContainer<?, ?, ?, ?> container) {
		assertThat(actual.getName()).as(descriptionText() + " Name").isEqualTo(container.getName());
		return this;
	}
}
