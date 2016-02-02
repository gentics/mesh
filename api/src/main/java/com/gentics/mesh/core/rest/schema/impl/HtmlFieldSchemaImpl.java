package com.gentics.mesh.core.rest.schema.impl;

import java.util.Optional;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModelImpl;

public class HtmlFieldSchemaImpl extends AbstractFieldSchema implements HtmlFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

	@Override
	public Optional<SchemaChangeModelImpl> compareTo(FieldSchema fieldSchema) {
		// TODO Auto-generated method stub
		return null;
	}
}
