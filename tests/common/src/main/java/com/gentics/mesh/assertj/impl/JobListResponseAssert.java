package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;

public class JobListResponseAssert extends AbstractAssert<JobListResponseAssert, JobListResponse> {

	public JobListResponseAssert(JobListResponse actual) {
		super(actual, JobListResponseAssert.class);
	}

	/**
	 * Asserts that all infos lists the status as completed.
	 * 
	 * @return Fluent API
	 */
	public JobListResponseAssert listsAll(JobStatus status) {
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
		assertEquals("The status did not contain the expected amount of infos. Json {\n" + actual.toJson(false) + "\n}", count,
				actual.getMetainfo().getTotalCount());
		return this;
	}

	/**
	 * 
	 * Assert that the given jobs are listed within the response.g
	 * 
	 * @param jobUuids
	 * @return Fluent API
	 */
	public JobListResponseAssert containsJobs(String... jobUuids) {
		List<String> list = actual.getData().stream().map(info -> info.getUuid()).collect(Collectors.toList());
		assertThat(list).as("List of jobs").contains(jobUuids);
		return this;
	}

	/**
	 * Assert that the response lists no jobs.
	 * 
	 * @return Fluent API
	 */
	public JobListResponseAssert isEmpty() {
		assertEquals(0, actual.getMetainfo().getTotalCount());
		return this;
	}

}
