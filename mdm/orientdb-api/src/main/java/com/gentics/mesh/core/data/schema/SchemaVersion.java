package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;

public interface SchemaVersion
		extends GraphFieldSchemaContainerVersion<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchemaVersion, HibSchema>, HibSchemaVersion {

	static final TypeInfo TYPE_INFO = new TypeInfo(SCHEMAVERSION, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}
}
