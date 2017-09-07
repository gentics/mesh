package com.gentics.mesh.core.rest.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
	@JsonPropertyDescription("The release reference information of the job.")
	private String releaseUuid;

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

	public String getReleaseUuid() {
		return releaseUuid;
	}

	public void setReleaseUuid(String releaseUuid) {
		this.releaseUuid = releaseUuid;
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

}
