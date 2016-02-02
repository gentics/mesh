package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * @see RemoveFieldChange
 */
public class RemoveFieldChangeImpl extends AbstractSchemaFieldChange implements RemoveFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.REMOVEFIELD;

	@Override
	public Schema apply(Schema schema) {
		schema.removeField(getFieldName());
		return schema;
	}
}
