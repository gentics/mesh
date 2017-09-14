package com.gentics.mesh.core.rest.job;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * POJO for job information.
 */
public class JobResponse extends AbstractResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("User reference of the creator of the element.")
	private UserReference creator;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 formatted created date string.")
	private String created;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The error message of the job.")
	private String errorMessage;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The detailed error information of the job.")
	private String errorDetail;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The type of the job.")
	private MigrationType type;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Migration status.")
	private MigrationStatus status;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Properties of the job.")
	private Map<String, String> properties = new HashMap<>();

	@JsonProperty(required = true)
	@JsonPropertyDescription("The stop date of the job.")
	private String stopDate;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The start date of the job.")
	private String startDate;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The completion count of the job. This indicates how many items the job has processed.")
	private long completionCount;

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getErrorDetail() {
		return errorDetail;
	}

	public void setErrorDetail(String errorDetail) {
		this.errorDetail = errorDetail;
	}

	public MigrationType getType() {
		return type;
	}

	public void setType(MigrationType type) {
		this.type = type;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public UserReference getCreator() {
		return creator;
	}

	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	public MigrationStatus getStatus() {
		return status;
	}

	public void setStatus(MigrationStatus status) {
		this.status = status;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getStopDate() {
		return stopDate;
	}

	public void setStopDate(String stopDate) {
		this.stopDate = stopDate;
	}

	public void setCompletionCount(long completionCount) {
		this.completionCount = completionCount;
	}

}
