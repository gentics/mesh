package com.gentics.mesh.core.job;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class JobTest extends AbstractMeshTest {

	@Test
	public void testJob() {
		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			Job job = root.enqueueReleaseMigration(user(), initialRelease());
			assertEquals("The creator of the job was not correct", user().getUuid(), job.getCreator().getUuid());
			assertNotNull("The creation timestamp was not set.", job.getCreationTimestamp());
			assertNotNull("The uuid of the job was not set.", job.getUuid());
			assertEquals("The job release information did not match.", initialReleaseUuid(), job.getRelease().getUuid());
			assertEquals("The job type did not match.", MigrationType.release, job.getType());
			assertNull("The job error detail should be null since it has not yet been marked as failed.", job.getErrorDetail());
			assertNull("The job error message should be null since it has not yet been marked as failed.", job.getErrorMessage());

			// Now test the failure information
			job.markAsFailed(new Exception("some error"));
			assertEquals("The job did not contain the correct error message", "some error", job.getErrorMessage());
			assertNotNull("The job did not contain error detail information.", job.getErrorDetail());
			tx.success();
		}

		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			List<? extends Job> list = root.findAll();
			assertThat(list).hasSize(1);
			Job job = list.get(0);
			assertEquals("some error", job.getErrorMessage());

			// Verify the transformation to rest
			JobResponse response = job.transformToRestSync(null, 0);
			assertEquals("some error", response.getErrorMessage());
			assertNotNull(response.getErrorDetail());
			assertEquals(job.getErrorDetail(), response.getErrorDetail());
			assertEquals(job.getType(), response.getType());
			assertEquals(job.getCreationDate(), response.getCreated());
			assertEquals(user().getUuid(), response.getCreator().getUuid());
		}
	}
}
