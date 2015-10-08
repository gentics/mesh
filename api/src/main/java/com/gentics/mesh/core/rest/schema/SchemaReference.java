package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * POJO that is used to model a schema reference within a node.
 */
public class SchemaReference extends NameUuidReference<SchemaReference> {

	public SchemaReference(String name, String uuid) {
		super(name, uuid);
	}

	public SchemaReference() {
	}
}
