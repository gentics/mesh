package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.SchemaModel;

/**
 * A GraphDB counterpart to {@link HibUpdateSchemaChange}.
 */
public interface UpdateSchemaChange extends FieldSchemaContainerUpdateChange<SchemaModel>, HibUpdateSchemaChange {

}
