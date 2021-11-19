package com.gentics.mesh.search.index;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.MathUtil;

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
			long total = createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(total).toList().blockingGet();
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
			long total = createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(total).toList().blockingGet();
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
			CommonTx ctx = tx.unwrap();
			PersistingUserDao userDao = ctx.userDao();

			// Delete all users to get empty set of vertices to work with
			for (HibUser user : userDao.findAll().list()) {
				userDao.delete(user, new DummyBulkActionContext());
			}
			long userCount = ctx.count(userDao.getPersistenceClass());
			assertEquals(0, userCount);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(userCount).toList().blockingGet();
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
			long total = createUsers(nUsers);

			// Test bulk manager
			BucketManager bulkManager = mesh().bucketManager();
			List<Bucket> buckets = bulkManager.getBuckets(total).toList().blockingGet();
			assertBuckets(buckets, syncBatchSize);
			// The bucket count is computed by dividing the element count by the batch size
			long expectedBucketsCount = MathUtil.ceilDiv(nUsers + 4, syncBatchSize);
			assertEquals(expectedBucketsCount, buckets.size());
		}
	}

	private void assertBuckets(List<Bucket> buckets, int batchSize) {
		Bucket prev = null;
		for (Bucket bucket : buckets) {
			// System.out.println(bucket);
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
		CommonTx ctx = CommonTx.get();
		PersistingUserDao userDao = ctx.userDao();

		// Create extra users
		for (int i = 0; i < nUsers; i++) {
			HibUser user = userDao.create("Anton" + i, user());
			assertNotNull(user.getBucketId());
		}
		long userCount = ctx.count(userDao.getPersistenceClass());
		// 4 = admin, anonymous + two test users
		assertEquals(nUsers + 4, userCount);
		return userCount;
	}

}
