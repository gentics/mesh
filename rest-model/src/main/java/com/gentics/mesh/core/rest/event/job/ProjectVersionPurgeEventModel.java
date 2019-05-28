package com.gentics.mesh.core.rest.event.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.event.AbstractProjectEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;

public class ProjectVersionPurgeEventModel extends AbstractProjectEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the job at the time when the event was send.")
	private JobStatus status;

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public JobStatus getStatus() {
		return status;
	}

}
