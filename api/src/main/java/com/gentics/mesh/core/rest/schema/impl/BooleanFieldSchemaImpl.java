package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.io.IOException;
import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

public class BooleanFieldSchemaImpl extends AbstractFieldSchema implements BooleanFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) throws IOException {
		if (fieldSchema instanceof BooleanFieldSchema) {

			SchemaChangeModel change = new SchemaChangeModel(SchemaChangeOperation.UPDATEFIELD, fieldSchema.getName());
			if (compareRequiredField(change, fieldSchema, false)) {
				change.loadMigrationScript();
				return Optional.of(change);
			}
		} else {
			return createTypeChange(fieldSchema);
		}
		return Optional.empty();
	}

}
