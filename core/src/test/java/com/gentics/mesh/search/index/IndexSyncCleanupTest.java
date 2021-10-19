package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
/**
 * Test that bogus indices will be detected and removed during index sync operation.
 */
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.parameter.client.IndexMaintenanceParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.helper.ExpectedEvent;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class IndexSyncCleanupTest extends AbstractMeshTest {

	@Before
	public void setup() throws Exception {
		getProvider().clear().blockingAwait();
		syncIndex();
		grantAdmin();
	}

	@Test
	public void testIndexPurge() throws Exception {
		List<String> extraIndices = new ArrayList<>();

		extraIndices.add("node-blub");
		extraIndices.add("node-blub2");
		extraIndices.add(User.composeIndexName() + "2");
		extraIndices.add(Group.composeIndexName() + "2");
		extraIndices.add(Role.composeIndexName() + "2");
		extraIndices.add(TagFamily.composeIndexName(projectUuid()) + "bogus");
		extraIndices.add(MicroschemaContainer.composeIndexName() + "bogus");
		extraIndices.add(SchemaContainer.composeIndexName() + "bogus");
		extraIndices.add(SchemaContainer.composeIndexName() + "bogus");
		extraIndices.add(Project.composeIndexName() + "bogus");

		// 1. Create extra bogus indices
		for (String idx : extraIndices) {
			createIndex(idx);
		}
		// The different index has no prefixed type and should be ignored
		createIndex("different");

		// Create thirdparty index
		createThirdPartyIndex("thirdparty");

		// Assert that indices have been created
		assertThat(indices())
			.contains("thirdparty")
			.containsAll(extraIndices.stream().map(e -> "mesh-" + e)
				.collect(Collectors.toSet()));

		// Invoke the sync
		waitForEvent(INDEX_SYNC_FINISHED, () -> {
			call(() -> client().invokeIndexSync());
		});

		// Check that all bogus indices have been removed and correct indices remain.
		List<String> remainingIndices = new ArrayList<>();
		remainingIndices.add("mesh-" + User.composeIndexName());
		remainingIndices.add("mesh-" + Group.composeIndexName());
		remainingIndices.add("mesh-" + Role.composeIndexName());
		remainingIndices.add("mesh-" + SchemaContainer.composeIndexName());
		remainingIndices.add("mesh-" + MicroschemaContainer.composeIndexName());
		remainingIndices.add("mesh-" + Project.composeIndexName());
		remainingIndices.add("mesh-" + TagFamily.composeIndexName(projectUuid()));
		remainingIndices.add("mesh-" + Tag.composeIndexName(projectUuid()));
		assertThat(indices())
			.doesNotContainAnyElementsOf(extraIndices)
			.contains("mesh-different", "thirdparty")
			.containsAll(remainingIndices);

	}

	/**
	 * Test that synchronizing indices restricted to name only synchronizes the specified index
	 * @throws Exception
	 */
	@Test
	public void testSyncWithName() throws Exception {
		runSyncTest(false);
	}

	/**
	 * Test that synchronizing indices restricted to name only synchronizes the specified index
	 * @throws Exception
	 */
	@Test
	public void testSyncWithFullName() throws Exception {
		runSyncTest(true);
	}

	/**
	 * Run the sync test
	 * @param prefix true to use the prefixed index name, false to use the bare index name
	 * @throws Exception
	 */
	protected void runSyncTest(boolean prefix) throws Exception {
		int timeout = 10_000;
		String index = prefix ? "mesh-user" : "user";

		// drop indices for user and project
		deleteIndex("mesh-user", "mesh-project");
		// recreate invalid indices for user and project
		createThirdPartyIndex("mesh-" + User.composeIndexName());
		createThirdPartyIndex("mesh-" + Project.composeIndexName());

		try (ExpectedEvent syncFinished = expectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeout)) {
			call(() -> client().invokeIndexSync(new IndexMaintenanceParametersImpl().setIndex(index)));
		}

		// check that the project is not found in index
		assertDocumentDoesNotExist(Project.composeIndexName(), Project.composeDocumentId(projectUuid()));
		// check that the user is found in index
		assertDocumentExists(User.composeIndexName(), User.composeDocumentId(userUuid()));
	}

	private void createThirdPartyIndex(String name) throws HttpErrorException {
		ElasticsearchClient<JsonObject> searchClient = searchProvider().getClient();
		JsonObject response = searchClient.createIndex(name, new JsonObject()).sync();
		assertTrue(response.getBoolean("acknowledged"));
	}

	private void createIndex(String name) {
		searchProvider().createIndex(new IndexInfo(name, new JsonObject(), new JsonObject(), "")).blockingAwait();
	}

	private void deleteIndex(String...indexNames) throws HttpErrorException {
		ElasticsearchClient<JsonObject> searchClient = searchProvider().getClient();
		searchClient.deleteIndex(indexNames).sync();
	}

	public Set<String> indices() throws HttpErrorException {
		ElasticsearchClient<JsonObject> searchClient = searchProvider().getClient();
		JsonObject indicesAfter = searchClient.readIndex("*").sync();
		return indicesAfter.fieldNames();
	}
}
