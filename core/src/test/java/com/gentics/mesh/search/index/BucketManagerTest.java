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
	 * Assert that two buckets will be generated when batch size is 10 and only 15 elements exist.
	 */
	@Test
	public void testSmallerBatchSizeVsElementCount() {
		int syncBatchSize = 10;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			int nUsers = 11;
			createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(UserImpl.class).toList().blockingGet();
			assertBuckets(buckets, syncBatchSize);
			int expectedBucketCount = 2;
			assertEquals(expectedBucketCount, buckets.size());
		}

	}

	/**
	 * Assert that a batch size of 0 automatically results in the use of a single bucket.
	 */
	@Test
	public void testBatchSizeZero() {
		int syncBatchSize = 0;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			int nUsers = 500;
			createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(UserImpl.class).toList().blockingGet();
			assertBuckets(buckets, syncBatchSize);
			int expectedBucketCount = 1;
			assertEquals(expectedBucketCount, buckets.size());
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
			List<Bucket> buckets = bulkManager.getBuckets(UserImpl.class).toList().blockingGet();
			assertBuckets(buckets, syncBatchSize);
			int expectedBucketsCount = 1;
			assertEquals(expectedBucketsCount, buckets.size());
		}
	}

	@Test
	public void testManager() {
		int syncBatchSize = 100;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			// Create extra users
			int nUsers = 500;
			createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(UserImpl.class).toList().blockingGet();
			assertBuckets(buckets, syncBatchSize);
			int expectedBucketsCount = (nUsers / syncBatchSize) + 1;
			assertEquals(expectedBucketsCount, buckets.size());
		}
	}

	private void assertBuckets(List<Bucket> buckets, int batchSize) {
		Bucket prev = null;
		for (Bucket bucket : buckets) {
			//System.out.println(bucket);
			if (prev == null) {
				assertEquals("The first bucket did not start at 0", 0, bucket.start());
			} else {
				assertEquals("The buckets did not connect as expected", prev.end(), bucket.start() - 1);
			}
			prev = bucket;
		}
		assertEquals("The last bucket did not end with maxInt.", Integer.MAX_VALUE, prev.end());
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
