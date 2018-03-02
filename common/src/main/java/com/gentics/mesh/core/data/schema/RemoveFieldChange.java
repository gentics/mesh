package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field removal.
 */
public interface RemoveFieldChange extends SchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.REMOVEFIELD;

}
