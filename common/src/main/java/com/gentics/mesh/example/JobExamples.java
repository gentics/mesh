package com.gentics.mesh.example;

import static com.gentics.mesh.example.ExampleUuids.BRANCH_UUID;
import static com.gentics.mesh.example.ExampleUuids.JOB_UUID;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_VEHICLE_UUID;

import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;

public class JobExamples extends AbstractExamples {

	public JobListResponse createJobList() {
		JobListResponse list = new JobListResponse();
		list.getData().add(createJobResponse());
		setPaging(list, 1, 1, 25, 1);
		return list;
	}

	public JobResponse createJobResponse() {
		JobResponse response = new JobResponse();
		response.setUuid(JOB_UUID);
		response.setCreator(createUserReference());
		response.setCreated(createNewTimestamp());
		response.setType(JobType.branch);
		response.getProperties().put("branchUuid", BRANCH_UUID);
		response.getProperties().put("schemaUuid", SCHEMA_VEHICLE_UUID);
		response.getProperties().put("fromVersion", "1.0");
		response.getProperties().put("toVersion", "2.0");
		return response;
	}

}
