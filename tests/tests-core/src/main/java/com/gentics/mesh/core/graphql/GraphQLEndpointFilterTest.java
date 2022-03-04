package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointFilterTest extends AbstractMeshTest {

	@Test
	public void testIsContainerFilter() {
		String queryName = "filtering/isContainer";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		JsonArray nodes = json.getJsonObject("data").getJsonObject("nodes").getJsonArray("elements");
		assertThat(nodes.size()).isGreaterThan(0);
		nodes.forEach(node -> {
			JsonObject nodeObj = (JsonObject) node;
			assertThat(nodeObj.getBoolean("isContainer")).isTrue();
		});
	}

	@Test
	public void testFilterOfNonDefaultLanguageNode() throws IOException {
		createNodeOfNonDefaultLanguage();

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery("filtering/nodes-de-field")));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions("filtering/nodes-de-field");
	}

	@Test
	public void testFilterOfNonDefaultLanguageNodeWithCorrectLanguage() throws IOException {
		createNodeOfNonDefaultLanguage();

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery("filtering/nodes-de-field-correct-language")));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions("filtering/nodes-de-field-correct-language");
	}

	private void createNodeOfNonDefaultLanguage() {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchemaName("folder");
		request.setLanguage("de");
		request.setParentNodeUuid(folderUuid());
		request.setFields(FieldMap.of(
			"name", StringField.of("deFieldTest")
		));
		client().createNode(PROJECT_NAME, request).blockingAwait();
	}
}
