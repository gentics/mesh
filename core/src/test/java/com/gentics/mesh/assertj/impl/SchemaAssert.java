package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaAssert extends AbstractMeshAssert<SchemaAssert, Schema> {

	public SchemaAssert(Schema actual) {
		super(actual, SchemaAssert.class);
	}

	/**
	 * Assert that the field schema with the given name is present.
	 * 
	 * @param fieldName
	 * @return Fluent API
	 */
	public SchemaAssert hasField(String fieldName) {
		assertTrue("The field {" + fieldName + "} could not be found.", actual.getFieldSchema(fieldName).isPresent());
		return this;
	}

	/**
	 * Assert that the field schema with the given name is not present.
	 * 
	 * @param string
	 * @return
	 */
	public SchemaAssert hasNoField(String fieldName) {
		assertFalse("The field {" + fieldName + "} could be found.", actual.getFieldSchema(fieldName).isPresent());
		return this;
	}

}
