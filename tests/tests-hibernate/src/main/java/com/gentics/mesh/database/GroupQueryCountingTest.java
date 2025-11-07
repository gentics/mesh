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

import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
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
public class GroupQueryCountingTest extends AbstractGroupQueryCountingTest {

	protected final static Map<String, Consumer<GroupResponse>> fieldAsserters = Map.of(
		"name", group -> {
			assertThat(group.getName()).as("Group name").isNotEmpty();
		},
		"roles", group -> {
			assertThat(group.getRoles()).as("Group roles").isNotNull().hasSize(NUM_ROLES_PER_GROUP);
		},
		"editor", group -> {
			assertThat(group.getEditor()).as("Group editor").isNotNull();
		},
		"edited", group -> {
			assertThat(group.getEdited()).as("Group edited").isNotEmpty();
		},
		"creator", group -> {
			assertThat(group.getCreator()).as("Group creator").isNotNull();
		},
		"created", group -> {
			assertThat(group.getCreated()).as("Group created").isNotEmpty();
		},
		"perms", group -> {
			assertThat(group.getPermissions()).as("Group permissions").isNotNull();
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
		GroupListResponse groupList = doTest(() -> client().findGroups(new GenericParametersImpl().setETag(etag).setFields("uuid", field)), 7, 1);
		assertThat(groupList.getData()).as("List of fetched groups").hasSize(totalNumGroups);

		for (GroupResponse group : groupList.getData()) {
			if (!initialGroupUuids.contains(group.getUuid())) {
				fieldAsserters.get(field).accept(group);
			}
		}
	}

	@Test
	public void testGetPage() {
		GroupListResponse groupList = doTest(() -> client().findGroups(new GenericParametersImpl().setETag(etag).setFields("uuid", field), new PagingParametersImpl().setPerPage(10L)), 8, 1);
		assertThat(groupList.getData()).as("List of fetched groups").hasSize(10);

		for (GroupResponse group : groupList.getData()) {
			if (!initialGroupUuids.contains(group.getUuid())) {
				fieldAsserters.get(field).accept(group);
			}
		}
	}
}
