package com.gentics.mesh.core.rest.event.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;

/**
 * Event model POJO for project perm events.
 */
public class PermissionChangedProjectElementEventModel extends PermissionChangedEventModelImpl implements MeshProjectElementEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project.")
	private ProjectReference project;

	@Override
	public ProjectReference getProject() {
		return project;
	}

	@Override
	public void setProject(ProjectReference project) {
		this.project = project;
	}
}
