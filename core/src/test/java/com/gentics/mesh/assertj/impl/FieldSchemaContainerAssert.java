package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

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
		assertTrue("The field {" + fieldName + "} could not be found.", actual.getFieldSchema(fieldName).isPresent());
		return this;
	}

	/**
	 * Assert that the field schema with the given name is not present.
	 * 
	 * @param string
	 * @return
	 */
	public FieldSchemaContainerAssert hasNoField(String fieldName) {
		assertFalse("The field {" + fieldName + "} could be found.", actual.getFieldSchema(fieldName).isPresent());
		return this;
	}

}
