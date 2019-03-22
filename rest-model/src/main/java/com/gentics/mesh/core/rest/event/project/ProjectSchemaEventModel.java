package com.gentics.mesh.core.rest.event.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class ProjectSchemaEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project.")
	private ProjectReference project;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference schema;

	public ProjectSchemaEventModel() {

	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public ProjectReference getProject() {
		return project;
	}

	public void setProject(ProjectReference project) {
		this.project = project;
	}
}
