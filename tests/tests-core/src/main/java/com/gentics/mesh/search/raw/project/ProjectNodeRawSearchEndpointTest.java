package com.gentics.mesh.search.raw.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, startServer = true, testSize = FULL)
public class ProjectNodeRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {

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
		call(() -> client().createNode("projectB", request));

		waitForSearchIdleEvent();

		// search in old project
		JsonObject response = new JsonObject(
			call(() -> client().searchNodesRaw("projectA", getSimpleQuery("fields.content", contentFieldValue))).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", nodeA.getUuid() + "-en", "The correct element was not found.");
		String path = complianceMode() == ComplianceMode.ES_6 ? "responses[0].hits.total" : "responses[0].hits.total.value";
		assertThat(response).has(path, "1", "Not exactly one item was found");
	}
}
