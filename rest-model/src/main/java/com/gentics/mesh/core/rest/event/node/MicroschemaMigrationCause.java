package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class MicroschemaMigrationCause extends MicroschemaMigrationMeshEventModel implements EventCauseInfo {

	@JsonCreator
	public MicroschemaMigrationCause(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project,
		String uuid, MigrationStatus status, MicroschemaReference fromVersion, MicroschemaReference toVersion) {
		super(origin, cause, event, branch, project, uuid, status, fromVersion, toVersion);
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.MICROSCHEMA_MIGRATION;
	}
}
