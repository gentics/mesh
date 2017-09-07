package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;

public class JobExamples extends AbstractExamples {

	public JobListResponse createJobList() {
		JobListResponse list = new JobListResponse();
		list.getData().add(createJobResponse());
		setPaging(list, 1, 1, 25, 1);
		return list;
	}

	public JobResponse createJobResponse() {
		JobResponse response = new JobResponse();
		response.setUuid(randomUUID());
		response.setCreator(createUserReference());
		response.setCreated(createTimestamp());
		response.setType(MigrationType.release);
		response.setReleaseUuid(randomUUID());
		return response;
	}

}
