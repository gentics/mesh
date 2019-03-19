package com.gentics.mesh.core.rest.event.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

public class MicroschemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the source microschema version.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference fromVersion;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the target microschema version.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference toVersion;

	@JsonCreator
	public MicroschemaMigrationMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project,
		String uuid, MigrationStatus status, MicroschemaReference fromVersion, MicroschemaReference toVersion) {
		super(origin, cause, event, branch, project, uuid, status);
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
	}

	public MicroschemaReference getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(MicroschemaReference fromVersion) {
		this.fromVersion = fromVersion;
	}

	public MicroschemaReference getToVersion() {
		return toVersion;
	}

	public void setToVersion(MicroschemaReference toVersion) {
		this.toVersion = toVersion;
	}
}
