package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.SchemaChangeOperation;

/**
 * Change entry which contains information for a field removal.
 */
public class RemoveFieldChange extends AbstractFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.REMOVEFIELD;
}
