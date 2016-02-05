package com.gentics.mesh.core.data.schema.handler;


import java.util.Optional;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

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
	public Optional<SchemaChangeModel> compare(FieldSchema fieldSchemaA, FieldSchema fieldSchemaB) {
		if (fieldSchemaA != null && fieldSchemaB != null) {
			return fieldSchemaA.compareTo(fieldSchemaB);
		} else if (fieldSchemaA != null && fieldSchemaB == null) {
			return Optional.of(new SchemaChangeModel(SchemaChangeOperation.REMOVEFIELD));
		} else if (fieldSchemaA == null && fieldSchemaB != null) {
			return Optional.of(new SchemaChangeModel(SchemaChangeOperation.ADDFIELD));
		}

		return Optional.empty();
	}
}
