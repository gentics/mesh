package com.gentics.mesh.search.index;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class BucketManagerTest extends AbstractMeshTest {

	/**
	 * Assert that a batch size of 0 automatically results in the use of a single partition.
	 */
	@Test
	public void testBatchSizeZero() {
		int syncBatchSize = 0;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			int nUsers = 500;
			long userCount = createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<BucketPartition> partitions = bulkManager.getBucketPartitions(UserImpl.class).toList().blockingGet();
			assertPartitions(partitions);
			int expectedPartitionCount = 1;
			assertEquals(expectedPartitionCount, bulkManager.getBucketPartitionCount(userCount));
			assertEquals(expectedPartitionCount, partitions.size());
		}
	}

	@Test
	public void testManagerForNoElements() {
		int syncBatchSize = 100;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			MeshRoot root = meshRoot();

			// Delete all users to get empty set of vertices to work with
			for (User user : root.getUserRoot().findAll().list()) {
				user.delete();
			}
			long userCount = db().count(UserImpl.class);
			assertEquals(0, userCount);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<BucketPartition> partitions = bulkManager.getBucketPartitions(UserImpl.class).toList().blockingGet();
			assertPartitions(partitions);
			int expectedPartitionCount = 1;
			assertEquals(expectedPartitionCount, bulkManager.getBucketPartitionCount(userCount));
			assertEquals(expectedPartitionCount, partitions.size());
		}
	}

	@Test
	public void testManager() {
		int syncBatchSize = 100;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			// Create extra users
			int nUsers = 500;
			long userCount = createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<BucketPartition> partitions = bulkManager.getBucketPartitions(UserImpl.class).toList().blockingGet();
			assertPartitions(partitions);
			int expectedPartitionCount = nUsers / syncBatchSize;
			assertEquals(expectedPartitionCount, bulkManager.getBucketPartitionCount(userCount));
			assertEquals(expectedPartitionCount, partitions.size());
		}
	}

	private void assertPartitions(List<BucketPartition> partitions) {
		BucketPartition prev = null;
		for (BucketPartition partition : partitions) {
			if (prev == null) {
				assertEquals("The first partition did not start at 0", 0, partition.start());
			} else {
				assertEquals("The partions did not connect as expected", prev.end(), partition.start() - 1);
			}
			prev = partition;
		}
		assertEquals("The last partition did not end with maxInt.", Integer.MAX_VALUE, prev.end());
	}

	private long createUsers(int nUsers) {
		MeshRoot root = meshRoot();

		// Create extra users
		for (int i = 0; i < nUsers; i++) {
			User user = root.getUserRoot().create("Anton" + i, user());
			assertNotNull(user.getBucketId());
		}
		long userCount = db().count(UserImpl.class);
		// 4 = admin, anonymous + two test users
		assertEquals(nUsers + 4, userCount);
		return userCount;
	}

}
