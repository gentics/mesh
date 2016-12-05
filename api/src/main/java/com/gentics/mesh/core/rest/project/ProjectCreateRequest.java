package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	private SchemaReference schema;

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schemaReference) {
		this.schema = schemaReference;
	}
}
