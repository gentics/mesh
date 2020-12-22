package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;

/**
 * @see DateFieldSchema
 */
public class DateFieldSchemaImpl extends AbstractFieldSchema implements DateFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.DATE.toString();
	}
}
