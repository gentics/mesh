package com.gentics.mesh.core.rest.schema.impl;

import java.io.IOException;

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
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (!(fieldSchema instanceof DateFieldSchema)) {
			return createTypeChange(fieldSchema);
		}
		return change;
	}

}
