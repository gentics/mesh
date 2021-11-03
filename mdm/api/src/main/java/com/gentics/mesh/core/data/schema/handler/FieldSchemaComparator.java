package com.gentics.mesh.core.data.schema.handler;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * The field schema comparator can be used to compare two field schemas with each other.
 */
public class FieldSchemaComparator {

	/**
	 * Compare two field schemas and return a schema change if both schemas don't match to each other.
	 * 
	 * @param fieldSchemaA
	 *            First field schema
	 * @param fieldSchemaB
	 *            Second field schema
	 * @return Detected changes
	 */
	public SchemaChangeModel compare(FieldSchema fieldSchemaA, FieldSchema fieldSchemaB) {
		if (fieldSchemaA != null && fieldSchemaB != null) {
			SchemaChangeModel change = fieldSchemaA.compareTo(fieldSchemaB);
			return change;
		} else if (fieldSchemaA != null && fieldSchemaB == null) {
			SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange(fieldSchemaA.getName());
			return change;
		} else if (fieldSchemaA == null && fieldSchemaB != null) {
			return SchemaChangeModel.createAddFieldChange(fieldSchemaB.getName(), fieldSchemaB.getType(),
					fieldSchemaB.getLabel());
		}

		return null;
	}
}
