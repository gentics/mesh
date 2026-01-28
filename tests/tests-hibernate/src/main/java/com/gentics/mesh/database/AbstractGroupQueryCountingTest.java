package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;

import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;

public abstract class AbstractGroupQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_GROUPS = 53;

	public final static int NUM_ROLES_PER_GROUP = 10;

	protected static int totalNumGroups = NUM_GROUPS;

	protected static Set<String> initialGroupUuids = new HashSet<>();

	@Before
	public void setup() {
		if (getTestContext().needsSetup()) {
			GroupListResponse initialGroups = call(() -> client().findGroups());
			initialGroupUuids.addAll(initialGroups.getData().stream().map(GroupResponse::getUuid).toList());
			totalNumGroups += initialGroups.getMetainfo().getTotalCount();

			// create groups
			for (int i = 0; i < NUM_GROUPS; i++) {
				GroupResponse group = createGroup("group_%d".formatted(i));

				// create and assign roles
				for (int j = 0; j < NUM_ROLES_PER_GROUP; j++) {
					createRole("group_%d_role_%d".formatted(i, j), group.getUuid());
				}
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

}
