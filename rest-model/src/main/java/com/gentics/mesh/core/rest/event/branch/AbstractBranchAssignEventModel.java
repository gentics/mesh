package com.gentics.mesh.core.rest.event.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.project.ProjectReference;

public abstract class AbstractBranchAssignEventModel<T extends NameUuidReference<T>> extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project to which the element belonged.")
	private ProjectReference project;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the branch.")
	private BranchReference branch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema that was assigned.")
	private T schema;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the migration job that has been created when assigning the schema.")
	private MigrationStatus status;

	public AbstractBranchAssignEventModel(String origin, EventCauseInfo cause, MeshEvent event, ProjectReference project, BranchReference branch,
		T schema, MigrationStatus status) {
		super(origin, cause, event);
		this.project = project;
		this.branch = branch;
		this.schema = schema;
		this.status = status;
	}

	public BranchReference getBranch() {
		return branch;
	}

	public void setBranch(BranchReference branch) {
		this.branch = branch;
	}

	public T getSchema() {
		return schema;
	}

	public void setSchema(T schema) {
		this.schema = schema;
	}

	public void setStatus(MigrationStatus status) {
		this.status = status;
	}

	public MigrationStatus getStatus() {
		return status;
	}

	public ProjectReference getProject() {
		return project;
	}

	public void setProject(ProjectReference project) {
		this.project = project;
	}

}
