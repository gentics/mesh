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
		response.setType(MigrationType.branch);
		response.getProperties().put("branchUuid", randomUUID());
		response.getProperties().put("schemaUuid", randomUUID());
		response.getProperties().put("fromVersion", "1.0");
		response.getProperties().put("toVersion", "2.0");
		return response;
	}

}
