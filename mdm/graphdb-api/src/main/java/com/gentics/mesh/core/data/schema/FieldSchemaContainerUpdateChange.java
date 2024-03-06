package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

/**
 * A GraphDB counterpart to {@link HibFieldSchemaContainerUpdateChange}.
 */
public interface FieldSchemaContainerUpdateChange<T extends FieldSchemaContainer> extends SchemaChange<T>, HibFieldSchemaContainerUpdateChange<T> {

}
