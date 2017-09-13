package com.gentics.mesh.core.rest.release.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.common.AbstractNameUuidReference;

public class AbstractReleaseSchemaInfo<T> extends AbstractNameUuidReference<T> {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The version of the microschema.")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Status of the migration which was triggered when the schema/microschema was added to the release.")
	private MigrationStatus migrationStatus;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Uuid of the migration job.")
	private String jobUuid;

	public String getVersion() {
		return version;
	}

	public AbstractReleaseSchemaInfo<T> setVersion(String version) {
		this.version = version;
		return this;
	}

	public MigrationStatus getMigrationStatus() {
		return migrationStatus;
	}

	public void setMigrationStatus(MigrationStatus status) {
		this.migrationStatus = status;
	}

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
	}
}
