package com.gentics.mesh.core.rest.schema.impl;

import java.io.IOException;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class BooleanFieldSchemaImpl extends AbstractFieldSchema implements BooleanFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.BOOLEAN.toString();
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (!(fieldSchema instanceof BooleanFieldSchema)) {
			return createTypeChange(fieldSchema);
		}
		return change;
	}

}
