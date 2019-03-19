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
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class SchemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the source schema version.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference fromVersion;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the target schema version.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference toVersion;

	@JsonCreator
	public SchemaMigrationMeshEventModel(String origin, EventCauseInfo cause, MeshEvent event, BranchReference branch, ProjectReference project,
		String uuid, MigrationStatus status, SchemaReference fromVersion, SchemaReference toVersion) {
		super(origin, cause, event, branch, project, uuid, status);
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
	}

	public SchemaReference getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(SchemaReference fromVersion) {
		this.fromVersion = fromVersion;
	}

	public SchemaReference getToVersion() {
		return toVersion;
	}

	public void setToVersion(SchemaReference toVersion) {
		this.toVersion = toVersion;
	}

}
