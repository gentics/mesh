package com.gentics.mesh.search.index;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class BulkManagerTest extends AbstractMeshTest {

	@Test
	public void testBulkManager() {
		int syncBatchSize = 100;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		try (Tx tx = tx()) {
			MeshRoot root = meshRoot();

			int nUsers = 500;
			for (int i = 0; i < nUsers; i++) {
				User user = root.getUserRoot().create("Anton" + i, user());
				assertNotNull(user.getBucketId());
			}
			long userCount = db().count(UserImpl.class);
			// 4 = admin, anonymous + two test users
			assertEquals(nUsers + 4, userCount);

			BucketManager bulkManager = mesh().bucketManager();
			int bulkMax = nUsers / syncBatchSize;
			assertEquals(bulkMax, bulkManager.getBucketSize(userCount));
		}
	}

}
