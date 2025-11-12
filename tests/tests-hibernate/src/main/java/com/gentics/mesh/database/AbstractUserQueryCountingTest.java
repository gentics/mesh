package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * Abstract base for query counting tests for Users
 */
public abstract class AbstractUserQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_USERS = 53;

	public final static int NUM_GROUPS_PER_USER = 10;

	protected static int totalNumUsers = NUM_USERS;

	protected static Set<String> initialUserUuids = new HashSet<>();

	protected static GroupResponse commonGroup;

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			UserListResponse initialUsers = call(() -> client().findUsers());
			initialUserUuids.addAll(initialUsers.getData().stream().map(UserResponse::getUuid).toList());
			totalNumUsers += initialUsers.getMetainfo().getTotalCount();

			// create a common group
			commonGroup = createGroup("common");

			// create users
			for (int i = 0; i < NUM_USERS; i++) {
				UserResponse user = createUser("user_%d".formatted(i), null);

				// assign the common group
				call(() -> client().addUserToGroup(commonGroup.getUuid(), user.getUuid()));

				// create and assign groups
				for (int j = 0; j < NUM_GROUPS_PER_USER; j++) {
					GroupResponse group = createGroup("user_%d_group_%d".formatted(i, j));
					call(() -> client().addUserToGroup(group.getUuid(), user.getUuid()));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}
