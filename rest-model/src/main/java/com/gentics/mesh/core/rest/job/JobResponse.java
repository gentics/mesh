package com.gentics.mesh.core.rest.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.job.warning.JobWarning;
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
	private JobType type;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Migration status.")
	private JobStatus status;

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

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of warnings which were encoutered while executing the job.")
	private List<JobWarning> warnings = new ArrayList<>();

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

	public JobType getType() {
		return type;
	}

	public void setType(JobType type) {
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
	public JobStatus getStatus() {
		return status;
	}

	/**
	 * Set the current status of the job.
	 * 
	 * @param status
	 */
	public void setStatus(JobStatus status) {
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

	/**
	 * Return the list of job warnings.
	 * 
	 * @return
	 */
	public List<JobWarning> getWarnings() {
		return warnings;
	}

	/**
	 * Add a new warning to the list of job warnings.
	 * 
	 * @param warning
	 */
	public void addWarning(JobWarning warning) {
		this.warnings.add(warning);
	}

	/**
	 * Set the list of warnings.
	 * 
	 * @param warnings
	 */
	public void setWarnings(List<JobWarning> warnings) {
		this.warnings = warnings;
	}
}
