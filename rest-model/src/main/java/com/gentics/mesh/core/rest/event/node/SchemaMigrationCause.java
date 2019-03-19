package com.gentics.mesh.core.rest.event.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class SchemaMigrationCause extends SchemaMigrationMeshEventModel implements EventCauseInfo {

	@JsonCreator
	public SchemaMigrationCause(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project, String uuid,
		MigrationStatus status, SchemaReference fromVersion, SchemaReference toVersion) {
		super(origin, cause, event, branch, project, uuid, status, fromVersion, toVersion);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ElementType getType() {
		return ElementType.JOB;
	}

	@Override
	public EventCauseAction getAction() {
		return EventCauseAction.SCHEMA_MIGRATION;
	}
}
