package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import java.util.Arrays;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class NodeRawSearchEndpointTest extends AbstractMeshTest {

	/**
	 * Verify that the global node search would find both nodes in both projects.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRawSearch() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		final String contentFieldValue = "Enemenemuh";
		final String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		request.getFields().put("teaser", FieldUtil.createStringField("teaserValue"));
		request.getFields().put("content", FieldUtil.createStringField(contentFieldValue));

		// projectA
		ProjectResponse projectA = createProject("projectA");
		call(() -> client().assignSchemaToProject("projectA", contentSchemaUuid));
		request.setParentNodeUuid(projectA.getRootNode().getUuid());
		NodeResponse nodeA = call(() -> client().createNode("projectA", request));

		// projectB
		ProjectResponse projectB = createProject("projectB");
		call(() -> client().assignSchemaToProject("projectB", contentSchemaUuid));
		request.setParentNodeUuid(projectB.getRootNode().getUuid());
		NodeResponse nodeB = call(() -> client().createNode("projectB", request));

		// We only want to test with our two projects.
		call(() -> client().deleteProject(projectUuid()));

		// search in old project
		JsonObject response = call(() -> client().searchNodesRaw(getSimpleQuery("fields.content", contentFieldValue)));
		assertThat(response).has("responses[0].hits.total", "2", "Not exactly two item was found.");
		String uuid1 = response.getJsonArray("responses").getJsonObject(0).getJsonObject("hits").getJsonArray("hits").getJsonObject(0).getString("_id");
		String uuid2 = response.getJsonArray("responses").getJsonObject(0).getJsonObject("hits").getJsonArray("hits").getJsonObject(1).getString("_id");
		assertThat(Arrays.asList(uuid1, uuid2)).containsExactlyInAnyOrder(nodeA.getUuid() + "-en", nodeB.getUuid() + "-en");

	}

	@Test
	public void testManySchemaSearch() {
		for (int i = 0; i < 45; i++) {
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName("dummy" + i);
			request.addField(FieldUtil.createHtmlFieldSchema("content"));
			SchemaResponse response = call(() -> client().createSchema(request));
			call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));
		}

		call(() -> client().searchNodesRaw(PROJECT_NAME, getSimpleQuery("fields.content", "the"), new PagingParametersImpl()
			.setPage(1).setPerPage(2), new VersioningParametersImpl().draft()));
	}
}
