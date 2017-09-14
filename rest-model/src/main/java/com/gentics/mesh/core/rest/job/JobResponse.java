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

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the Gentics Mesh instance on which the job was executed.")
	private String nodeName;

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

	/**
	 * Return the date on which the job was queued/created.
	 * 
	 * @return
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * Set the date on which the job was queued/created.
	 * 
	 * @param created
	 */
	public void setCreated(String created) {
		this.created = created;
	}

	/**
	 * Return the creator of the job.
	 * 
	 * @return
	 */
	public UserReference getCreator() {
		return creator;
	}

	/**
	 * Set the creator of the job.
	 * 
	 * @param creator
	 */
	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	/**
	 * Return the current status of the job.
	 * 
	 * @return
	 */
	public MigrationStatus getStatus() {
		return status;
	}

	/**
	 * Set the current status of the job.
	 * 
	 * @param status
	 */
	public void setStatus(MigrationStatus status) {
		this.status = status;
	}

	/**
	 * Return the date on which the job was started.
	 * 
	 * @return
	 */
	public String getStartDate() {
		return startDate;
	}

	/**
	 * Set the date on which the job was started.
	 * 
	 * @param startDate
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	/**
	 * Return the date on which the job finished.
	 * 
	 * @return
	 */
	public String getStopDate() {
		return stopDate;
	}

	/**
	 * Set the job stop date.
	 * 
	 * @param stopDate
	 */
	public void setStopDate(String stopDate) {
		this.stopDate = stopDate;
	}

	/**
	 * Return the amount of elements which were processed by the job.
	 * 
	 * @return
	 */
	public long getCompletionCount() {
		return completionCount;
	}

	/**
	 * Set the amount of elements which were processed by the job.
	 * 
	 * @param completionCount
	 */
	public void setCompletionCount(long completionCount) {
		this.completionCount = completionCount;

	}

	/**
	 * Return the name of the Gentics Mesh node on which the job was executed.
	 * 
	 * @return
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * Set the name on which the job was executed.
	 * 
	 * @param nodeName
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

}
