package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.JsonFieldSchema;

/**
 * @see JsonFieldSchema
 */
public class JsonFieldSchemaImpl extends AbstractFieldSchema implements JsonFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.JSON.toString();
	}

}
