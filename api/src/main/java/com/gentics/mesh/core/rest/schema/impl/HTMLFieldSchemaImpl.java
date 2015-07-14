package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;

public class HTMLFieldSchemaImpl extends AbstractFieldSchema implements HTMLFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}
}
