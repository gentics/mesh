package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.EMPTY;

import java.io.IOException;
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
	public SchemaChangeModel compare(FieldSchema fieldSchemaA, FieldSchema fieldSchemaB) throws IOException {
		if (fieldSchemaA != null && fieldSchemaB != null) {
			SchemaChangeModel change = fieldSchemaA.compareTo(fieldSchemaB);
			// Only load the migration script if a change has been detected
			if (change.getOperation() != EMPTY) {
				change.loadMigrationScript();
			}
			return change;
		} else if (fieldSchemaA != null && fieldSchemaB == null) {
			SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange(fieldSchemaA.getName());
			change.loadMigrationScript();
			return change;
		} else if (fieldSchemaA == null && fieldSchemaB != null) {
			return SchemaChangeModel.createAddFieldChange(fieldSchemaB.getName(), fieldSchemaB.getType(), fieldSchemaB.getLabel());
		}

		return null;
	}
}
