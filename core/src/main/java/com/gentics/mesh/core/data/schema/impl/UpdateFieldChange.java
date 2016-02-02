package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This can include field specific settings or even a field type change.
 */
public class UpdateFieldChange extends AbstractFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;
}
