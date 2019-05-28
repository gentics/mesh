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

import org.junit.Test;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class JobEndpointTest extends AbstractMeshTest {

	@Test
	public void testListJobs() {

		call(() -> client().findJobs(), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		JobListResponse jobList = call(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		waitForJobs(() -> {
			SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
			schema.setName("folder2");
			call(() -> client().updateSchema(uuid, schema));
		}, COMPLETED, 1);

		tx((tx) -> {
			boot().jobRoot().enqueueBranchMigration(user(), initialBranch());
		});

		jobList = call(() -> client().findJobs());
		JobResponse job = jobList.getData().get(1);
		assertThat(job.getProperties()).doesNotContainKey("microschemaUuid");
		assertThat(job.getProperties()).doesNotContainKey("microschemaName");
		assertThat(job.getProperties()).containsKey("schemaName");
		assertThat(job.getProperties()).containsKey("schemaUuid");
		assertThat(jobList.getData()).hasSize(2);
	}

	@Test
	public void testDeleteFailedJob() {

		String jobUuid = tx(() -> boot().jobRoot().enqueueBranchMigration(user(), initialBranch()).getUuid());

		call(() -> client().deleteJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		call(() -> client().deleteJob(jobUuid), BAD_REQUEST, "job_error_invalid_state", jobUuid);

		triggerAndWaitForJob(jobUuid, FAILED);

		call(() -> client().deleteJob(jobUuid));

		JobListResponse jobList = call(() -> client().findJobs());
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

		tx(() -> group().addRole(roles().get("admin")));

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
		tx(() -> group().addRole(roles().get("admin")));

		call(() -> client().findJobByUuid("bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testManualInvoke() {
		String jobUuid = tx(() -> boot().jobRoot().enqueueBranchMigration(user(), initialBranch()).getUuid());

		call(() -> client().invokeJobProcessing(), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

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

		tx(() -> group().addRole(roles().get("admin")));

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

		grantAdminRole();

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

		grantAdminRole();

		triggerAndWaitForJob(jobUuid, FAILED);

		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		// Change the job so that it will no longer fail
		tx(()-> {
			Branch branch = project().getBranchRoot().create("testBranch", user(), null, true, initialBranch(), EventQueueBatch.create());
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
		tx(() -> group().addRole(roles().get("admin")));
		JobListResponse status = call(() -> client().findJobs());
		assertEquals(0, status.getMetainfo().getTotalCount());
	}

}
