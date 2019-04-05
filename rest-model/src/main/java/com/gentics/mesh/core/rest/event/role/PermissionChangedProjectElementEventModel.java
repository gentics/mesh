package com.gentics.mesh.core.rest.event.role;

import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class PermissionChangedProjectElementEventModel extends PermissionChangedEventModelImpl implements MeshProjectElementEventModel {

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
