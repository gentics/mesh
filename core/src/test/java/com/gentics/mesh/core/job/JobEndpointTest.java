package com.gentics.mesh.core.job;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.JobParameters;
import com.gentics.mesh.parameter.client.JobParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class JobEndpointTest extends AbstractMeshTest {

	@Test
	public void testListJobs() {

		call(() -> client().findJobs(), FORBIDDEN, "error_admin_permission_required");

		JobListResponse jobList = adminCall(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		waitForJob(() -> {
			SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
			schema.setName("folder2");
			call(() -> client().updateSchema(uuid, schema));
		});

		tx((tx) -> {
			boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
		});

		jobList = adminCall(() -> client().findJobs());
		Optional<JobResponse> optionalSchemaMigrationJob = jobList.getData().stream().filter(job -> job.getType() == JobType.schema).findFirst();
		assertThat(optionalSchemaMigrationJob).as("Schema migration job").isPresent();
		JobResponse job = optionalSchemaMigrationJob.get();
		assertThat(job.getProperties()).doesNotContainKey("microschemaUuid");
		assertThat(job.getProperties()).doesNotContainKey("microschemaName");
		assertThat(job.getProperties()).containsKey("schemaName");
		assertThat(job.getProperties()).containsKey("schemaUuid");
		assertThat(jobList.getData()).hasSize(2);
	}

	/**
	 * Test filtering by status and/or type when getting the jobs list
	 */
	@Test
	public void testListJobsFiltered() {
		JobListResponse jobList = adminCall(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		waitForJob(() -> {
			SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
			schema.setName("folder2");
			call(() -> client().updateSchema(uuid, schema));
		});

		tx((tx) -> {
			boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
		});

		JobResponse branchMigrationJob = new JobResponse();
		branchMigrationJob.setType(JobType.branch);
		branchMigrationJob.setStatus(JobStatus.QUEUED);

		JobResponse schemaMigrationJob = new JobResponse();
		schemaMigrationJob.setType(JobType.schema);
		schemaMigrationJob.setStatus(JobStatus.COMPLETED);

		// test with different filters
		List<Pair<Consumer<JobParameters>, List<JobResponse>>> tests = Arrays.asList(
				Pair.of(p -> {}, Arrays.asList(branchMigrationJob, schemaMigrationJob)),
				Pair.of(p -> p.setStatus(JobStatus.FAILED), Collections.emptyList()),
				Pair.of(p -> p.setStatus(JobStatus.COMPLETED), Arrays.asList(schemaMigrationJob)),
				Pair.of(p -> p.setStatus(JobStatus.QUEUED), Arrays.asList(branchMigrationJob)),
				Pair.of(p -> p.setStatus(JobStatus.COMPLETED, JobStatus.QUEUED), Arrays.asList(schemaMigrationJob, branchMigrationJob)),
				Pair.of(p -> p.setType(JobType.versionpurge), Collections.emptyList()),
				Pair.of(p -> p.setType(JobType.schema), Arrays.asList(schemaMigrationJob)),
				Pair.of(p -> p.setType(JobType.branch), Arrays.asList(branchMigrationJob)),
				Pair.of(p -> p.setType(JobType.branch, JobType.schema), Arrays.asList(branchMigrationJob, schemaMigrationJob)),
				Pair.of(p -> p.setType(JobType.branch).setStatus(JobStatus.COMPLETED), Collections.emptyList()),
				Pair.of(p -> p.setType(JobType.branch).setStatus(JobStatus.QUEUED), Arrays.asList(branchMigrationJob))
			);

		for (Pair<Consumer<JobParameters>, List<JobResponse>> test : tests) {
			JobParametersImpl parameters = new JobParametersImpl();
			test.getLeft().accept(parameters);
			String description = null;
			if (parameters.getStatus().isEmpty() && parameters.getType().isEmpty()) {
				description = "Unfiltered job list";
			} else {
				description = "Job List filtered for";
				if (!parameters.getStatus().isEmpty()) {
					description += " status=" + parameters.getStatus();
				}
				if (!parameters.getType().isEmpty()) {
					description += " type=" + parameters.getType();
				}
			}
			jobList = adminCall(() -> client().findJobs(parameters));
			assertThat(jobList.getData()).as(description).usingElementComparatorOnFields("status", "type")
					.containsOnlyElementsOf(test.getRight());
		}
	}

	@Test
	public void testDeleteFailedJob() {

		String jobUuid = tx(() -> boot().jobRoot().enqueueBranchMigration(user(), initialBranch()).getUuid());

		call(() -> client().deleteJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		adminCall(() -> client().deleteJob(jobUuid), BAD_REQUEST, "job_error_invalid_state", jobUuid);

		triggerAndWaitForJob(jobUuid, FAILED);

		adminCall(() -> client().deleteJob(jobUuid));

		JobListResponse jobList = adminCall(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

	}

	/**
	 * Verify that no failed jobs are executed again. Those jobs must be ignored by the job worker verticle.
	 */
	@Test
	public void testHandlingOfFailedJobs() {

		String jobUuid = tx(() -> {
			Job job = boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		grantAdmin();

		waitForJob(() -> {
			GenericMessageResponse msg = call(() -> client().invokeJobProcessing());
			assertThat(msg).matches("job_processing_invoked");
		}, jobUuid, FAILED);

		call(() -> client().invokeJobProcessing());
		TestUtils.sleep(5_000);
		JobListResponse status = call(() -> client().findJobs());
		assertEquals("No other migration should have been executed.", 1, status.getMetainfo().getTotalCount());
		assertEquals(jobUuid, status.getData().get(0).getUuid());

	}

	@Test
	public void testLoadBogusJob() {
		grantAdmin();
		call(() -> client().findJobByUuid("bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testManualInvoke() {
		String jobUuid = tx(() -> boot().jobRoot().enqueueBranchMigration(user(), initialBranch()).getUuid());

		call(() -> client().invokeJobProcessing(), FORBIDDEN, "error_admin_permission_required");

		grantAdmin();
		JobResponse response = waitForJob(() -> {
			GenericMessageResponse message = call(() -> client().invokeJobProcessing());
			assertThat(message).matches("job_processing_invoked");
		}, jobUuid, FAILED);
		assertEquals("The job uuid of the job should match up with the migration status info uuid.", jobUuid, response.getUuid());

	}

	@Test
	public void testReadJob() {
		String jobUuid = tx(() -> {
			Job job = boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		tx(() -> {
			SchemaContainer schema = schemaContainer("content");
			Job job = boot().jobRoot().enqueueSchemaMigration(user(), initialBranch(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		String job3Uuid = tx(() -> {
			SchemaContainer schema = schemaContainer("folder");
			Job job = boot().jobRoot().enqueueSchemaMigration(user(), initialBranch(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		grantAdmin();

		JobResponse jobResponse = call(() -> client().findJobByUuid(job3Uuid));
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("folder");
			assertEquals(initialBranchUuid(), jobResponse.getProperties().get("branchUuid"));
			assertEquals(schema.getUuid(), jobResponse.getProperties().get("schemaUuid"));
			assertEquals(schema.getLatestVersion().getVersion(), jobResponse.getProperties().get("fromVersion"));
			assertEquals(schema.getLatestVersion().getVersion(), jobResponse.getProperties().get("toVersion"));
		}

		jobResponse = call(() -> client().findJobByUuid(jobUuid));
		assertEquals(initialBranchUuid(), jobResponse.getProperties().get("branchUuid"));

	}

	@Test
	public void testRetryJob() {

		String jobUuid = tx(() -> {
			Job job = boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		call(() -> client().resetJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		grantAdmin();

		triggerAndWaitForJob(jobUuid, FAILED);

		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		call(() -> client().resetJob(jobUuid));

		jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNull(jobResonse.getErrorMessage());
		assertEquals("After reset the job must be 'queued'", QUEUED, jobResonse.getStatus());
	}

	@Test
	public void testProcessJob() {
		Job job = tx(() -> boot().jobRoot().enqueueBranchMigration(user(), initialBranch()));
		String jobUuid = tx(() -> job.getUuid());

		call(() -> client().processJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		grantAdmin();

		triggerAndWaitForJob(jobUuid, FAILED);

		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		// Change the job so that it will no longer fail
		tx(() -> {
			Branch branch = project().getBranchRoot().create("testBranch", user(), null, true, initialBranch(), createBatch());
			job.setBranch(branch);
		});

		waitForJob(() -> {
			call(() -> client().processJob(jobUuid));
		}, jobUuid, COMPLETED);

		jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNull(jobResonse.getErrorMessage());
		assertEquals("After process the job must be 'completed'", COMPLETED, jobResonse.getStatus());
	}

	@Test
	public void testJobStatusWithNoMigrationRunning() {
		grantAdmin();
		JobListResponse status = call(() -> client().findJobs());
		assertEquals(0, status.getMetainfo().getTotalCount());
	}

}
