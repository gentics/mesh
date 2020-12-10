package com.gentics.mesh.core.rest.event.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

/**
 * POJO for microschema events.
 */
public class ProjectMicroschemaEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project.")
	private ProjectReference project;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the microschema.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference microschema;

	public ProjectMicroschemaEventModel() {

	}

	public ProjectReference getProject() {
		return project;
	}

	public void setProject(ProjectReference project) {
		this.project = project;
	}

	public MicroschemaReference getMicroschema() {
		return microschema;
	}

	public void setMicroschema(MicroschemaReference microschema) {
		this.microschema = microschema;
	}

}
