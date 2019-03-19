package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;

public class BranchMigrationCause extends BranchMigrationMeshEventModel implements EventCauseInfo {

	@JsonCreator
	public BranchMigrationCause(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project, String uuid,
		MigrationStatus status) {
		super(origin, cause, event, branch, project, uuid, status);
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.BRANCH_MIGRATION;
	}
}
