package com.gentics.mesh.core.rest.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the project")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the root node. Creating a project will also automatically create the base node of the project and link the schema to the initial release of the project.")
	private SchemaReference schema;

	/**
	 * Return the project name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the project name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public ProjectCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

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
	 * @return Fluent API
	 */
	public ProjectCreateRequest setSchema(SchemaReference schemaReference) {
		this.schema = schemaReference;
		return this;
	}
}