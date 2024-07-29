package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.parameter.client.IndexMaintenanceParametersImpl;
import com.gentics.mesh.test.helper.ExpectedEvent;
import org.junit.Before;
import org.junit.Test;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

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
		extraIndices.add(HibUser.composeIndexName() + "2");
		extraIndices.add(HibGroup.composeIndexName() + "2");
		extraIndices.add(HibRole.composeIndexName() + "2");
		extraIndices.add(HibTagFamily.composeIndexName(projectUuid()) + "bogus");
		extraIndices.add(HibMicroschema.composeIndexName() + "bogus");
		extraIndices.add(HibSchema.composeIndexName() + "bogus");
		extraIndices.add(HibSchema.composeIndexName() + "bogus");
		extraIndices.add(HibProject.composeIndexName() + "bogus");

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
		remainingIndices.add("mesh-" + HibUser.composeIndexName());
		remainingIndices.add("mesh-" + HibGroup.composeIndexName());
		remainingIndices.add("mesh-" + HibRole.composeIndexName());
		remainingIndices.add("mesh-" + HibSchema.composeIndexName());
		remainingIndices.add("mesh-" + HibMicroschema.composeIndexName());
		remainingIndices.add("mesh-" + HibProject.composeIndexName());
		remainingIndices.add("mesh-" + HibTagFamily.composeIndexName(projectUuid()));
		remainingIndices.add("mesh-" + HibTag.composeIndexName(projectUuid()));
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
		createThirdPartyIndex("mesh-" + HibUser.composeIndexName());
		createThirdPartyIndex("mesh-" + HibProject.composeIndexName());

		try (ExpectedEvent syncFinished = expectEvent(MeshEvent.INDEX_SYNC_FINISHED, timeout)) {
			call(() -> client().invokeIndexSync(new IndexMaintenanceParametersImpl().setIndex(index)));
		}

		// check that the project is not found in index
		assertDocumentDoesNotExist(HibProject.composeIndexName(), HibProject.composeDocumentId(projectUuid()));
		// check that the user is found in index
		assertDocumentExists(HibUser.composeIndexName(), HibUser.composeDocumentId(userUuid()));
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
