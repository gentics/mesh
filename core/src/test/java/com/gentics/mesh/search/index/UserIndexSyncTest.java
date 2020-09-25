package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class UserIndexSyncTest extends AbstractMeshTest {

	@Test
	public void testUserSync() throws Exception {
		int syncBatchSize = 10;
		options().getSearchOptions().setSyncBatchSize(syncBatchSize);
		String newUsername = "peterpan";
		int nUsers = 20;

		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			// Create extra users
			for (int i = 0; i < nUsers; i++) {
				userDao.create("Anton" + i, user());
			}
			tx.success();
		}

		// 1. Prepare indices and add users
		recreateIndices();

		UserListResponse userList = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", newUsername)));
		assertThat(userList.getData()).as("Search result").isEmpty();
		grantAdmin();

		// 2. Now clear all data
		searchProvider().clear().blockingAwait();

		// Create a new user
		String newUserUuid = tx(tx -> {
			UserDaoWrapper userDao = tx.userDao();
			RoleDaoWrapper roleDao = tx.roleDao();

			HibUser user = userDao.create(newUsername, user());
			roleDao.grantPermissions(role(), user, InternalPermission.values());
			return user.getUuid();
		});

		UserResponse newUser = call(() -> client().findUserByUuid(newUserUuid));

		// 3. Invoke Sync
		GenericMessageResponse message = call(() -> client().invokeIndexSync());
		assertThat(message).matches("search_admin_index_sync_invoked");
		waitForSearchIdleEvent();

		// 4. Assert user can be found
		userList = call(() -> client().searchUsers(getSimpleTermQuery("username.raw", newUsername)));
		assertEquals(1, userList.getData().size());
		assertThat(userList.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(newUser);

		// 5. Invoke Sync again
		message = call(() -> client().invokeIndexSync());
		assertThat(message).matches("search_admin_index_sync_invoked");
		waitForSearchIdleEvent();

		SearchStatusResponse status = call(() -> client().searchStatus());
		for (Map.Entry<String, EntityMetrics> entry : status.getMetrics().entrySet()) {
			String name = entry.getKey();
			EntityMetrics metric = entry.getValue();
			assertEquals("The type {" + name + "} should not track any deletes during the sync", 0, metric.getDelete().getSynced().longValue());
			assertEquals("The type {" + name + "} should not track any inserts during the sync", 0, metric.getInsert().getSynced().longValue());
			assertEquals("The type {" + name + "} should not track any updates during the sync", 0, metric.getUpdate().getSynced().longValue());
		}

	}
}
