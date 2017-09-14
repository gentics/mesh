package com.gentics.mesh.core.job;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.QUEUED;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
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

import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT_AND_NODE, startServer = true)
public class JobEndpointTest extends AbstractMeshTest {

	@Test
	public void testListJobs() {

		call(() -> client().findJobs(), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		JobListResponse jobList = call(() -> client().findJobs());
		assertThat(jobList.getData()).isEmpty();

		tx(() -> {
			boot().jobRoot().enqueueReleaseMigration(user(), initialRelease());
		});

		jobList = call(() -> client().findJobs());
		assertThat(jobList.getData()).hasSize(1);
	}

	@Test
	public void testPeriodicProcessing() {
		String jobUuid = tx(() -> boot().jobRoot().enqueueReleaseMigration(user(), initialRelease()).getUuid());
		tx(() -> group().addRole(roles().get("admin")));
		assertEquals(QUEUED, call(() -> client().findJobByUuid(jobUuid)).getStatus());
		
		// Wait the initial startup delay and after that the periodic delay
		TestUtils.sleep(30_000 + 30_000);

		assertEquals(FAILED, call(() -> client().findJobByUuid(jobUuid)).getStatus());
	}

	@Test
	public void testDeleteFailedJob() {

		String jobUuid = tx(() -> boot().jobRoot().enqueueReleaseMigration(user(), initialRelease()).getUuid());

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
			Job job = boot().jobRoot().enqueueReleaseMigration(user(), initialRelease());
			return job.getUuid();
		});

		tx(() -> group().addRole(roles().get("admin")));

		waitForJob(() -> {
			GenericMessageResponse msg = call(() -> client().invokeJobProcessing());
			assertMessage(msg, "job_processing_invoked");
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
		String jobUuid = tx(() -> boot().jobRoot().enqueueReleaseMigration(user(), initialRelease()).getUuid());

		call(() -> client().invokeJobProcessing(), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		JobResponse response = waitForJob(() -> {
			GenericMessageResponse msg = call(() -> client().invokeJobProcessing());
			assertMessage(msg, "job_processing_invoked");
		}, jobUuid, FAILED);
		assertEquals("The job uuid of the job should match up with the migration status info uuid.", jobUuid, response.getUuid());

	}

	@Test
	public void testReadJob() {
		String jobUuid = tx(() -> {
			Job job = boot().jobRoot().enqueueReleaseMigration(user(), initialRelease());
			return job.getUuid();
		});

		String job2Uuid = tx(() -> {
			SchemaContainer schema = schemaContainer("content");
			Job job = boot().jobRoot().enqueueSchemaMigration(user(), initialRelease(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		String job3Uuid = tx(() -> {
			SchemaContainer schema = schemaContainer("folder");
			Job job = boot().jobRoot().enqueueSchemaMigration(user(), initialRelease(), schema.getLatestVersion(), schema.getLatestVersion());
			return job.getUuid();
		});

		tx(() -> group().addRole(roles().get("admin")));

		JobResponse jobResponse = call(() -> client().findJobByUuid(job3Uuid));
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("folder");
			assertEquals(initialReleaseUuid(), jobResponse.getProperties().get("releaseUuid"));
			assertEquals(schema.getUuid(), jobResponse.getProperties().get("schemaUuid"));
			assertEquals(schema.getLatestVersion().getVersion(), jobResponse.getProperties().get("fromVersion"));
			assertEquals(schema.getLatestVersion().getVersion(), jobResponse.getProperties().get("toVersion"));
		}

		jobResponse = call(() -> client().findJobByUuid(jobUuid));
		assertEquals(initialReleaseUuid(), jobResponse.getProperties().get("releaseUuid"));

	}

	@Test
	public void testRetryJob() {

		String jobUuid = tx(() -> {
			Job job = boot().jobRoot().enqueueReleaseMigration(user(), initialRelease());
			return job.getUuid();
		});

		call(() -> client().resetJob(jobUuid), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		triggerAndWaitForJob(jobUuid, FAILED);

		JobResponse jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNotNull(jobResonse.getErrorMessage());

		call(() -> client().resetJob(jobUuid));

		jobResonse = call(() -> client().findJobByUuid(jobUuid));
		assertNull(jobResonse.getErrorMessage());

	}

	@Test
	public void testJobStatusWithNoMigrationRunning() {
		tx(() -> group().addRole(roles().get("admin")));
		JobListResponse status = call(() -> client().findJobs());
		assertEquals(0, status.getMetainfo().getTotalCount());
	}

}
