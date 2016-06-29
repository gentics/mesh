package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	private SchemaReference schemaReference;

	public SchemaReference getSchemaReference() {
		return schemaReference;
	}

	public void setSchemaReference(SchemaReference schemaReference) {
		this.schemaReference = schemaReference;
	}
}
