package com.gentics.mesh.core.rest.event.migration;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class BranchMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	public BranchMigrationMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project,
		String uuid, MigrationStatus status) {
		super(origin, cause, event, branch, project, uuid, status);
	}

}
