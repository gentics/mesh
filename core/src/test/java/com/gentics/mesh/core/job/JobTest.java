package com.gentics.mesh.core.job;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.junit.Test;

import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.job.impl.BranchMigrationJobImpl;
import com.gentics.mesh.core.data.job.impl.MicronodeMigrationJobImpl;
import com.gentics.mesh.core.data.job.impl.NodeMigrationJobImpl;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.exceptions.CompositeException;

@MeshTestSetting(testSize = FULL, startServer = false)
public class JobTest extends AbstractMeshTest {

	@Test
	public void testJob() {
		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			Job job = root.enqueueBranchMigration(user(), initialBranch());
			// Disabled in order to apply to contention fix
			assertNull("The creator should not be set.",job.getCreator());
			//assertEquals("The creator of the job was not correct", user().getUuid(), job.getCreator().getUuid());
			//assertNotNull("The creation timestamp was not set.", job.getCreationTimestamp());
			assertNotNull("The uuid of the job was not set.", job.getUuid());
			assertEquals("The job branch information did not match.", initialBranchUuid(), job.getBranch().getUuid());
			assertEquals("The job type did not match.", JobType.branch, job.getType());
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
			List<? extends Job> list = TestUtils.toList(root.findAll());
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
			//assertEquals(user().getUuid(), response.getCreator().getUuid());
		}
	}

	private Exception buildExceptionStackTraceLongerThan(int size) {
		List<Exception> exceptions = new ArrayList<>();
		do {
			exceptions.add(new Exception("Some Error"));
		} while (ExceptionUtils.getStackTrace(new CompositeException(exceptions)).length() < size);
		return new CompositeException(exceptions);
	}

	@Test
	public void testJobErrorDetailTruncate() {
		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			Job job = root.enqueueBranchMigration(user(), initialBranch());
			assertNull("The job error detail should be null since it has not yet been marked as failed.", job.getErrorDetail());
			Exception ex = buildExceptionStackTraceLongerThan(Job.ERROR_DETAIL_MAX_LENGTH * 2);
			assertThat(ExceptionUtils.getStackTrace(ex).length()).isGreaterThan(Job.ERROR_DETAIL_MAX_LENGTH);
			// Now test the if the error details are really truncated
			job.markAsFailed(ex);
			assertNotNull(job.getErrorDetail());
			assertThat(job.getErrorDetail().length()).isLessThanOrEqualTo(Job.ERROR_DETAIL_MAX_LENGTH);
			tx.success();
		}

		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			List<? extends Job> list = TestUtils.toList(root.findAll());
			assertThat(list).hasSize(1);
			Job job = list.get(0);

			// Verify the transformation to rest
			JobResponse response = job.transformToRestSync(null, 0);
			assertNotNull(response.getErrorDetail());
			assertThat(response.getErrorDetail().length()).isLessThanOrEqualTo(Job.ERROR_DETAIL_MAX_LENGTH);
		}
	}

	@Test
	public void testJobRootTypeHandling() {
		try (Tx tx = tx()) {
			JobRoot root = boot().jobRoot();
			root.addItem(tx.addVertex(NodeMigrationJobImpl.class));
			root.addItem(tx.addVertex(MicronodeMigrationJobImpl.class));
			root.addItem(tx.addVertex(BranchMigrationJobImpl.class));

			List<String> list = TestUtils.toList(root.findAll()).stream().map(i -> i.getClass().getName()).collect(Collectors.toList());
			assertThat(list).containsExactly(NodeMigrationJobImpl.class.getName(), MicronodeMigrationJobImpl.class.getName(),
					BranchMigrationJobImpl.class.getName());
		}
	}
}
