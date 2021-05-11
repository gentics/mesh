package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public class FieldSchemaContainerAssert extends AbstractObjectAssert<FieldSchemaContainerAssert, FieldSchemaContainer> {

	public FieldSchemaContainerAssert(FieldSchemaContainer actual) {
		super(actual, FieldSchemaContainerAssert.class);
	}

	/**
	 * Assert that the field schema with the given name is present.
	 * 
	 * @param fieldName
	 * @return Fluent API
	 */
	public FieldSchemaContainerAssert hasField(String fieldName) {
		assertNotNull("The field {" + fieldName + "} could not be found.", actual.getField(fieldName));
		return this;
	}

	/**
	 * Assert that the field schema with the given name is not present.
	 * 
	 * @param string
	 * @return
	 */
	public FieldSchemaContainerAssert hasNoField(String fieldName) {
		assertNull("The field {" + fieldName + "} could be found.", actual.getField(fieldName));
		return this;
	}

}
