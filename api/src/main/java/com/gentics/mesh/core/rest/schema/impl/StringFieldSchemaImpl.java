package com.gentics.mesh.core.rest.schema.impl;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;

public class StringFieldSchemaImpl extends AbstractFieldSchema implements StringFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.STRING.toString();
	}

	@Override
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		// TODO Auto-generated method stub
		return null;
	}
}
