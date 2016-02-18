package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

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
	 * @throws IOException
	 */
	public Optional<SchemaChangeModel> compare(FieldSchema fieldSchemaA, FieldSchema fieldSchemaB) throws IOException {
		if (fieldSchemaA != null && fieldSchemaB != null) {
			return fieldSchemaA.compareTo(fieldSchemaB);
		} else if (fieldSchemaA != null && fieldSchemaB == null) {
			SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange(fieldSchemaA.getName());
			change.loadMigrationScript();
			return Optional.of(change);
		} else if (fieldSchemaA == null && fieldSchemaB != null) {
			return Optional.of(SchemaChangeModel.createAddFieldChange(fieldSchemaB.getName(),  fieldSchemaB.getType()));
		}

		return Optional.empty();
	}
}
