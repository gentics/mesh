package com.gentics.mesh.core.data.schema.handler;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;

/**
 * The field schema comparator can be used to compare two field schemas with each other.
 */
@Component
public class FieldSchemaComparator {

	/**
	 * Compare two field schemas and return a schema change if both schemas don't match to each other.
	 * 
	 * @param fieldSchemaA
	 * @param fieldSchemaB
	 * @return
	 */
	public Optional<SchemaChangeModelImpl> compare(FieldSchema fieldSchemaA, FieldSchema fieldSchemaB) {
		return Optional.empty();
	}
}
