package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class SchemaChangeModelAssert extends AbstractAssert<SchemaChangeModelAssert, SchemaChangeModel> {

	public SchemaChangeModelAssert(SchemaChangeModel actual) {
		super(actual, SchemaChangeModelAssert.class);
	}

	public SchemaChangeModelAssert is(SchemaChangeOperation operation) {
		assertEquals("The operation of the change did not match the expected one.", operation, actual.getOperation());
		return this;
	}

	public SchemaChangeModelAssert forField(String fieldName) {
		assertNotNull("The field for this change could not be identified.", actual.getFieldName());
		assertEquals("The change field name does not match the expected one.", fieldName, actual.getFieldName());
		return this;
	}

	public SchemaChangeModelAssert hasProperty(String key, Object value) {
		assertTrue("The property with key {" + key + "} could not be found within the change.", actual.getProperties().containsKey(key));
		if (value instanceof String[]) {
			Object actualValue = actual.getProperties().get(key);
			if (actualValue instanceof List) {
				actualValue = ((List<Object>) actualValue).toArray();
			}
			assertArrayEquals("The value for the given property did not match the expected one.", (Object[]) value, (Object[]) actualValue);

		} else {
			assertEquals("The value for the given property did not match the expected one.", value, actual.getProperties().get(key));
		}
		return this;
	}

	public SchemaChangeModelAssert hasNoProperty(String key) {
		assertFalse("The property with key {" + key + "} could be found within the change.", actual.getProperties().containsKey(key));
		return this;
	}

}
