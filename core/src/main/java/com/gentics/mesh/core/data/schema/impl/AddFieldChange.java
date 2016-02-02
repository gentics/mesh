package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.SchemaChangeOperation;

/**
 * Change entry which contains information for a field to be added to the schema. 
 */
public class AddFieldChange extends AbstractFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.ADDFIELD;
}
