package com.gentics.mesh.core.rest.project;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	private SchemaReference schema;

	/**
	 * Return the schema reference for the project create request.
	 * 
	 * @return
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Set the schema reference for the project. The reference is used to define the type of root node of the project.
	 * 
	 * @param schemaReference
	 */
	public void setSchema(SchemaReference schemaReference) {
		this.schema = schemaReference;
	}
}
