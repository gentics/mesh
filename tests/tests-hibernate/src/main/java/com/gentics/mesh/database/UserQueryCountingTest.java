package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
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
public class UserQueryCountingTest extends AbstractUserQueryCountingTest {

	protected final static Map<String, Consumer<UserResponse>> fieldAsserters = Map.of(
		"username", user -> {
			assertThat(user.getUsername()).as("User name").isNotEmpty();
		},
		"groups", user -> {
			assertThat(user.getGroups()).as("User groups").isNotNull().hasSize(NUM_GROUPS_PER_USER + 1);
		},
		"rolesHash", user -> {
			assertThat(user.getRolesHash()).as("User rolesHash").isNotEmpty();
		},
		"editor", user -> {
			assertThat(user.getEditor()).as("User editor").isNotNull();
		},
		"edited", user -> {
			assertThat(user.getEdited()).as("User edited").isNotEmpty();
		},
		"creator", user -> {
			assertThat(user.getCreator()).as("User creator").isNotNull();
		},
		"created", user -> {
			assertThat(user.getCreated()).as("User created").isNotEmpty();
		},
		"perms", user -> {
			assertThat(user.getPermissions()).as("User permissions").isNotNull();
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

	@Test
	public void testGetAll() {
		UserListResponse userList = doTest(
				() -> client().findUsers(new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 8, 1);
		assertThat(userList.getData()).as("List of fetched users").hasSize(totalNumUsers);

		for (UserResponse user : userList.getData()) {
			if (!initialUserUuids.contains(user.getUuid())) {
				fieldAsserters.get(field).accept(user);
			}
		}
	}

	@Test
	public void testGetPage() {
		UserListResponse userList = doTest(
				() -> client().findUsers(new GenericParametersImpl().setETag(etag).setFields("uuid", field),
						new PagingParametersImpl().setPerPage(10L)),
				8, 1);
		assertThat(userList.getData()).as("List of fetched users").hasSize(10);

		for (UserResponse user : userList.getData()) {
			if (!initialUserUuids.contains(user.getUuid())) {
				fieldAsserters.get(field).accept(user);
			}
		}
	}
}
