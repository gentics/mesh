package com.gentics.mesh.core.rest.event.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.AbstractMeshEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.project.ProjectReference;

public abstract class AbstractMigrationMeshEventModel extends AbstractMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Branch to which the migration applies.")
	private BranchReference branch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Project to which the migration applies.")
	private ProjectReference project;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the corresponding job.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the migration at the time when the event was send.")
	private JobStatus status;

	/**
	 * Return the referenced branch.
	 * 
	 * @return
	 */
	public BranchReference getBranch() {
		return branch;
	}

	/**
	 * Set the referenced branch.
	 * 
	 * @param branch
	 */
	public void setBranch(BranchReference branch) {
		this.branch = branch;
	}

	/**
	 * Return the referenced project.
	 * 
	 * @return
	 */
	public ProjectReference getProject() {
		return project;
	}

	/**
	 * Set the referenced project.
	 * 
	 * @param project
	 */
	public void setProject(ProjectReference project) {
		this.project = project;
	}

	/**
	 * Return the job uuid.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the job uuid.
	 * 
	 * @param jobUuid
	 */
	public void setUuid(String jobUuid) {
		this.uuid = jobUuid;
	}

	/**
	 * Return the current migration status.
	 * 
	 * @return
	 */
	public JobStatus getStatus() {
		return status;
	}

	/**
	 * Set the migration status.
	 * 
	 * @param status
	 */
	public void setStatus(JobStatus status) {
		this.status = status;
	}

}
