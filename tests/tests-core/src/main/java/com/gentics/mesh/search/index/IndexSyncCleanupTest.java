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

import org.junit.Before;
import org.junit.Test;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class IndexSyncCleanupTest extends AbstractMeshTest {

	@Before
	public void setup() {
		grantAdmin();
	}

	@Test
	public void testIndexPurge() throws Exception {
		List<String> extraIndices = new ArrayList<>();

		extraIndices.add("node-blub");
		extraIndices.add("node-blub2");
		extraIndices.add(HibUser.composeIndexName() + "2");
		extraIndices.add(Group.composeIndexName() + "2");
		extraIndices.add(Role.composeIndexName() + "2");
		extraIndices.add(TagFamily.composeIndexName(projectUuid()) + "bogus");
		extraIndices.add(HibMicroschema.composeIndexName() + "bogus");
		extraIndices.add(Schema.composeIndexName() + "bogus");
		extraIndices.add(Schema.composeIndexName() + "bogus");
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
		remainingIndices.add("mesh-" + HibUser.composeIndexName());
		remainingIndices.add("mesh-" + Group.composeIndexName());
		remainingIndices.add("mesh-" + Role.composeIndexName());
		remainingIndices.add("mesh-" + Schema.composeIndexName());
		remainingIndices.add("mesh-" + HibMicroschema.composeIndexName());
		remainingIndices.add("mesh-" + Project.composeIndexName());
		remainingIndices.add("mesh-" + TagFamily.composeIndexName(projectUuid()));
		remainingIndices.add("mesh-" + Tag.composeIndexName(projectUuid()));
		assertThat(indices())
			.doesNotContainAnyElementsOf(extraIndices)
			.contains("mesh-different", "thirdparty")
			.containsAll(remainingIndices);

	}

	private void createThirdPartyIndex(String name) throws HttpErrorException {
		ElasticsearchClient<JsonObject> searchClient = searchProvider().getClient();
		JsonObject response = searchClient.createIndex(name, new JsonObject()).sync();
		assertTrue(response.getBoolean("acknowledged"));
	}

	private void createIndex(String name) {
		searchProvider().createIndex(new IndexInfo(name, new JsonObject(), new JsonObject(), "")).blockingAwait();
	}

	public Set<String> indices() throws HttpErrorException {
		ElasticsearchClient<JsonObject> searchClient = searchProvider().getClient();
		JsonObject indicesAfter = searchClient.readIndex("*").sync();
		return indicesAfter.fieldNames();
	}
}