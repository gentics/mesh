package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * POJO that is used to model a schema reference within a node. Only the name or the uuid of the schema must be supplied when this reference is being used
 * within a node create request / node update request.
 */
public class SchemaReference extends NameUuidReference<SchemaReference> {

}
