package com.gentics.mesh.core.rest.schema.impl;

import java.io.IOException;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

public class HtmlFieldSchemaImpl extends AbstractFieldSchema implements HtmlFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = super.compareTo(fieldSchema);
		if (!(fieldSchema instanceof HtmlFieldSchema)) {
			return createTypeChange(fieldSchema);
		}
		return change;
	}
}
