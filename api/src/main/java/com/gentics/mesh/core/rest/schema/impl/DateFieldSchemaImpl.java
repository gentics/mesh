package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class DateFieldSchemaImpl extends AbstractFieldSchema implements DateFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}

	@Override
	public Optional<SchemaChangeModel> compareTo(FieldSchema fieldSchema) {
		if (fieldSchema instanceof DateFieldSchema) {
			DateFieldSchema dateFieldSchema = (DateFieldSchema) fieldSchema;
			SchemaChangeModel change = new SchemaChangeModel(UPDATEFIELD, fieldSchema.getName());

			if (compareRequiredField(change, dateFieldSchema, false)) {
				return Optional.of(change);
			}

		} else {
			//TODO impl
		}
		return Optional.empty();
	}

}
