package com.gentics.mesh.core.rest.branch.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;
import com.gentics.mesh.core.rest.job.JobStatus;

public class AbstractBranchSchemaInfo<T> extends AbstractNameUuidReference<T> {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The version of the microschema.")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Status of the migration which was triggered when the schema/microschema was added to the branch.")
	private JobStatus migrationStatus;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Uuid of the migration job.")
	private String jobUuid;

	public String getVersion() {
		return version;
	}

	public AbstractBranchSchemaInfo<T> setVersion(String version) {
		this.version = version;
		return this;
	}

	public JobStatus getMigrationStatus() {
		return migrationStatus;
	}

	public void setMigrationStatus(JobStatus status) {
		this.migrationStatus = status;
	}

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
	}
}
