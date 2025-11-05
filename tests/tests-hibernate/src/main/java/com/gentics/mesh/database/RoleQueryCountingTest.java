package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed REST call queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class RoleQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_ROLES = 53;

	public final static int NUM_GROUPS_PER_ROLE = 10;

	protected static int totalNumRoles = NUM_ROLES;

	protected static Set<String> initialRoleUuids = new HashSet<>();

	protected final static Map<String, Consumer<RoleResponse>> fieldAsserters = Map.of(
		"name", role -> {
			assertThat(role.getName()).as("Role name").isNotEmpty();
		},
		"groups", role -> {
			assertThat(role.getGroups()).as("Role groups").isNotNull().hasSize(NUM_GROUPS_PER_ROLE);
		},
		"editor", role -> {
			assertThat(role.getEditor()).as("Role editor").isNotNull();
		},
		"edited", role -> {
			assertThat(role.getEdited()).as("Role edited").isNotEmpty();
		},
		"creator", role -> {
			assertThat(role.getCreator()).as("Role creator").isNotNull();
		},
		"created", role -> {
			assertThat(role.getCreated()).as("Role created").isNotEmpty();
		},
		"perms", role -> {
			assertThat(role.getPermissions()).as("Role permissions").isNotNull();
		}
	);

	@Parameters(name = "{index}: field {0}, etag {1}")
	public static Collection<Object[]> parameters() throws Exception {
		Collection<Object[]> data = new ArrayList<>();
		for (String field : fieldAsserters.keySet()) {
			for (Boolean etag : Arrays.asList(true, false)) {
				data.add(new Object[] {field, etag});
			}
		}
		return data;
	}

	@Parameter(0)
	public String field;

	@Parameter(1)
	public boolean etag;

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

	@Test
	public void testGetAll() {
		RoleListResponse roleList = doTest(() -> client().findRoles(new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 7, 1);
		assertThat(roleList.getData()).as("List of fetched roles").hasSize(totalNumRoles);

		for (RoleResponse role : roleList.getData()) {
			if (!initialRoleUuids.contains(role.getUuid())) {
				fieldAsserters.get(field).accept(role);
			}
		}
	}

	@Test
	public void testGetPage() {
		RoleListResponse roleList = doTest(
				() -> client().findRoles(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new PagingParametersImpl().setPerPage(10L)),
				7, 1);
		assertThat(roleList.getData()).as("List of fetched roles").hasSize(10);

		for (RoleResponse role : roleList.getData()) {
			if (!initialRoleUuids.contains(role.getUuid())) {
				fieldAsserters.get(field).accept(role);
			}
		}
	}
}
