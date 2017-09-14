package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;

public class JobListResponseAssert extends AbstractAssert<JobListResponseAssert, JobListResponse> {

	public JobListResponseAssert(JobListResponse actual) {
		super(actual, JobListResponseAssert.class);
	}

	/**
	 * Asserts that all infos lists the status as completed.
	 * 
	 * @return Fluent API
	 */
	public JobListResponseAssert listsAll(MigrationStatus status) {
		for (JobResponse info : actual.getData()) {
			assertEquals("Migration {" + info + "}", status, info.getStatus());
		}
		return this;
	}

	/**
	 * Asserts the amount of infos within the response.
	 * 
	 * @param count
	 * @return Fluent API
	 */
	public JobListResponseAssert hasInfos(int count) {
		assertEquals("The status did not contain the expected amount of infos.", count, actual.getMetainfo().getTotalCount());
		return this;
	}

}
