package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;

public abstract class AbstractRoleQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_ROLES = 53;

	public final static int NUM_GROUPS_PER_ROLE = 10;

	protected static int totalNumRoles = NUM_ROLES;

	protected static Set<String> initialRoleUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			RoleListResponse initialRoles = call(() -> client().findRoles());
			initialRoleUuids.addAll(initialRoles.getData().stream().map(RoleResponse::getUuid).toList());
			totalNumRoles += initialRoles.getMetainfo().getTotalCount();

			// create roles
			for (int i = 0; i < NUM_ROLES; i++) {
				RoleResponse role = createRole("role_%d".formatted(i), null);
	
				// create and assign groups
				for (int j = 0; j < NUM_GROUPS_PER_ROLE; j++) {
					GroupResponse group = createGroup("role_%d_group_%d".formatted(i, j));
					call(() -> client().addRoleToGroup(group.getUuid(), role.getUuid()));
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}
}
