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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.gentics.mesh.core.db.CommonTx;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.JobParameters;
import com.gentics.mesh.parameter.client.JobParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
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

		jobList = adminCall(() -> client().findJobs());
		Optional<JobResponse> optionalSchemaMigrationJob = jobList.getData().stream().filter(job -> job.getType() == JobType.schema).findFirst();
		assertThat(optionalSchemaMigrationJob).as("Schema migration job").isPresent();
		JobResponse job = optionalSchemaMigrationJob.get();
		assertThat(job.getProperties()).doesNotContainKey("microschemaUuid");
		assertThat(job.getProperties()).doesNotContainKey("microschemaName");
		assertThat(job.getProperties()).containsKey("schemaName");
		assertThat(job.getProperties()).containsKey("schemaUuid");
		assertThat(adminCall(() -> client().findJobs()).getData()).hasSize(1);

		tx((tx) -> {
			boot().jobDao().enqueueBranchMigration(user(), initialBranch());
		});

		assertThat(adminCall(() -> client().findJobs()).getData()).hasSize(2);
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
			boot().jobDao().enqueueBranchMigration(user(), initialBranch());
		});

		String branchName = tx(() -> initialBranch().getName());

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
				Pair.of(p -> p.setType(JobType.branch).setStatus(JobStatus.QUEUED), Arrays.asList(branchMigrationJob)),
				Pair.of(p -> p.setBranchName("doesnotexist"), Collections.emptyList()),
				Pair.of(p -> p.setBranchName(branchName), Arrays.asList(branchMigrationJob, schemaMigrationJob)),
				Pair.of(p -> p.setSchemaName("folder2"), Arrays.asList(schemaMigrationJob))
			);

		for (Pair<Consumer<JobParameters>, List<JobResponse>> test : tests) {
			JobParametersImpl parameters = new JobParametersImpl();
			test.getLeft().accept(parameters);
			String description = null;
			if (parameters.isEmpty()) {
				description = "Unfiltered job list";
			} else {
				description = "Job List filtered for";
				if (!parameters.getStatus().isEmpty()) {
					description += " status=" + parameters.getStatus();
				}
				if (!parameters.getType().isEmpty()) {
					description += " type=" + parameters.getType();
				}
				if (!parameters.getBranchName().isEmpty()) {
					description += " branchName=" + parameters.getBranchName();
				}
				if (!parameters.getBranchUuid().isEmpty()) {
					description += " branchUuid=" + parameters.getBranchUuid();
				}
				if (!parameters.getSchemaName().isEmpty()) {
					description += " schemaName=" + parameters.getSchemaName();
				}
				if (!parameters.getSchemaUuid().isEmpty()) {
					description += " schemaUuid=" + parameters.getSchemaUuid();
				}
				if (!parameters.getMicroschemaName().isEmpty()) {
					description += " microschemaName=" + parameters.getMicroschemaName();
				}
				if (!parameters.getMicroschemaUuid().isEmpty()) {
					description += " microschemaUuid=" + parameters.getMicroschemaUuid();
				}
				if (!parameters.getFromVersion().isEmpty()) {
					description += " fromVersion=" + parameters.getFromVersion();
				}
				if (!parameters.getToVersion().isEmpty()) {
					description += " toVersion=" + parameters.getToVersion();
				}
			}
			jobList = adminCall(() -> client().findJobs(parameters));
			assertThat(jobList.getData()).as(description).usingElementComparatorOnFields("status", "type")
					.containsOnlyElementsOf(test.getRight());
		}
	}

	@Test
	public void testDeleteFailedJob() {

		String jobUuid = tx(() -> boot().jobDao().enqueueBranchMigration(user(), initialBranch()).getUuid());

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
			HibJob job = boot().jobDao().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		waitForJob(() -> {
			GenericMessageResponse msg = adminCall(() -> client().invokeJobProcessing());
			assertThat(msg).matches("job_processing_invoked");
		}, jobUuid, FAILED);

		adminCall(() -> client().invokeJobProcessing());
		TestUtils.sleep(5_000);
		JobListResponse status = adminCall(() -> client().findJobs());
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
		String jobUuid = tx(() -> boot().jobDao().enqueueBranchMigration(user(), initialBranch()).getUuid());

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
			HibJob job = boot().jobDao().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		tx(() -> {
			HibSchema schema = schemaContainer("content");
			HibJob job = boot().jobDao().enqueueSchemaMigration(user(), initialBranch(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		String job3Uuid = tx(() -> {
			HibSchema schema = schemaContainer("folder");
			HibJob job = boot().jobDao().enqueueSchemaMigration(user(), initialBranch(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		grantAdmin();

		JobResponse jobResponse = call(() -> client().findJobByUuid(job3Uuid));
		try (Tx tx = tx()) {
			HibSchema schema = schemaContainer("folder");
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
			HibJob job = boot().jobDao().enqueueBranchMigration(user(), initialBranch());
			return job.getUuid();
		});

		call(() -> client().resetJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		triggerAndWaitForJob(jobUuid, FAILED);

		grantAdmin();
		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		call(() -> client().resetJob(jobUuid));

		jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNull(jobResonse.getErrorMessage());
		assertEquals("After reset the job must be 'queued'", QUEUED, jobResonse.getStatus());
	}

	@Test
	public void testProcessJob() {
		HibJob job = tx(() -> boot().jobDao().enqueueBranchMigration(user(), initialBranch()));
		String jobUuid = tx(() -> job.getUuid());

		call(() -> client().processJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		grantAdmin();

		triggerAndWaitForJob(jobUuid, FAILED);

		grantAdmin();

		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		// Change the job so that it will no longer fail
		tx(tx -> {
			BranchDao branchDao = tx.branchDao();
			HibBranch branch = branchDao.create(project(), "testBranch", user(), null, true, initialBranch(), createBatch());
			HibJob toUpdate = boot().jobDao().findByUuid(job.getUuid());
			toUpdate.setBranch(branch);
			CommonTx.get().jobDao().mergeIntoPersisted(toUpdate);
		});

		waitForJob(() -> {
			call(() -> client().processJob(jobUuid));
		}, jobUuid, COMPLETED);

		grantAdmin();
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
