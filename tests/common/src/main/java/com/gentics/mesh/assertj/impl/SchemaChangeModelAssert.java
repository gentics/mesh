package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEMICROSCHEMA;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

import io.vertx.core.json.JsonObject;

public class SchemaChangeModelAssert extends AbstractAssert<SchemaChangeModelAssert, SchemaChangeModel> {

	public SchemaChangeModelAssert(SchemaChangeModel actual) {
		super(actual, SchemaChangeModelAssert.class);
	}

	/**
	 * Assert that the change maps a specific schema operation.
	 * 
	 * @param operation
	 * @return
	 */
	public SchemaChangeModelAssert is(SchemaChangeOperation operation) {
		assertEquals("The operation of the change did not match the expected one.", operation, actual.getOperation());
		return this;
	}

	/**
	 * Assert that the change contains a schema field reference.
	 * 
	 * @param fieldName
	 * @return Fluent API
	 */
	public SchemaChangeModelAssert forField(String fieldName) {
		assertNotNull("The field for this change could not be identified.", actual.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		assertEquals("The change field name does not match the expected one.", fieldName, actual.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		return this;
	}

	/**
	 * Assert that the change contains the property with the given key and value.
	 * 
	 * @param key
	 * @param value
	 * @return Fluent API
	 */
	public SchemaChangeModelAssert hasProperty(String key, Object value) {
		assertTrue("The property with key {" + key + "} could not be found within the change.", actual.getProperties().containsKey(key));
		if (value instanceof String[]) {
			Object actualValue = actual.getProperties().get(key);
			if (actualValue instanceof List) {
				actualValue = ((List<Object>) actualValue).toArray();
			}
			// Construct debug information
			String values = "{";
			for (Object obj : (Object[]) actualValue) {
				values += "," + obj.toString();
			}
			values += "}";
			assertArrayEquals("The value for the given property did not match the expected one." + values, (Object[]) value, (Object[]) actualValue);
		} else if (value instanceof JsonObject) {
			Object current = actual.getProperties().get(key);
			if (current instanceof String) {
				current = new JsonObject((String) current);;
			}
			if (current instanceof LinkedHashMap) {
				current = new JsonObject((Map) current);
			}
			assertEquals("The value for the given property did not match the expected one.", ((JsonObject) value).encode(),
				((JsonObject) current).encode());
		} else {
			Object current = actual.getProperties().get(key);
			if (current instanceof ArrayList) {
				List<String> list = (ArrayList) current;
				assertEquals("The value for the given property did not match the expected one.", list, current);
			} else {
				assertEquals("The value for the given property did not match the expected one.", value, current);
			}
		}
		return this;
	}

	/**
	 * Assert that the change does not contain a property with the given key.
	 * 
	 * @param key
	 * @return
	 */
	public SchemaChangeModelAssert hasNoProperty(String key) {
		assertFalse("The property with key {" + key + "} could be found within the change.", actual.getProperties().containsKey(key));
		return this;
	}

	/**
	 * Assert that the change is an field container update operation (either UPDATESCHEMA or UPDATEMICROSCHEMA).
	 * 
	 * @param container
	 * @return
	 */
	public SchemaChangeModelAssert isUpdateOperation(Object container) {
		if (container instanceof SchemaModel) {
			assertEquals("The change operation does not match.", UPDATESCHEMA, actual.getOperation());
		} else {
			assertEquals("The change operation does not match.", UPDATEMICROSCHEMA, actual.getOperation());
		}
		return this;
	}

}
