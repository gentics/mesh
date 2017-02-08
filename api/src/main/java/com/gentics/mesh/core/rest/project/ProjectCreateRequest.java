package com.gentics.mesh.core.rest.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectCreateRequest extends ProjectUpdateRequest {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the root node. Creating a project will also automatically create the base node of the project and link the schema to the initial release of the project.")
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
