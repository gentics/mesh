package com.gentics.mesh.core.rest.event.migration;

import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;

public abstract class AbstractMigrationMeshEventModel extends AbstractMeshEventModel {

	private BranchReference branch;

	private ProjectReference project;

	public BranchReference getBranch() {
		return branch;
	}

	public void setBranch(BranchReference branch) {
		this.branch = branch;
	}

	public ProjectReference getProject() {
		return project;
	}

	public void setProject(ProjectReference project) {
		this.project = project;
	}

}
