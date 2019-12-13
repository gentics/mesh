package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEmptyNodeTest extends AbstractMeshTest {
	private NodeResponse deNode;

	@Before
	public void setUp() {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage("de")
			.setParentNodeUuid(folderUuid())
			.setSchemaName("folder");
		deNode = client().createNode(PROJECT_NAME, request).blockingGet();
	}

	@Test
	public void testSingleNode() throws IOException {
		graphql("nodeByUuid", "uuid", deNode.getUuid());
	}

	@Test
	public void testNodesByUuid() throws IOException {
		graphql("nodesByUuid", "uuid", deNode.getUuid());
	}

	@Test
	public void testAllNodes() throws IOException {
		GraphQLResponse allNodes = graphql("allNodes");

		JsonObject foundNode = findNodeByUuid(allNodes.getData());

		assertThat(foundNode.getString("language")).isNull();
		assertThat(foundNode.getJsonObject("fields")).isNull();
	}

	@Test
	public void testNodesFromSchema() throws IOException {
		GraphQLResponse allNodes = graphql("nodesFromSchema");

		JsonObject foundNode = findNodeByUuid(allNodes.getData().getJsonObject("schema"));

		assertThat(foundNode.getString("language")).isNull();
		assertThat(foundNode.getJsonObject("fields")).isNull();
	}

	private JsonObject findNodeByUuid(JsonObject schema) {
		return toStream(schema.getJsonObject("nodes").getJsonArray("elements"))
			.map(node -> (JsonObject) node)
			.filter(node -> node.getString("uuid").equals(deNode.getUuid()))
			.findFirst().get();
	}

	private GraphQLResponse graphql(String name) throws IOException {
		return graphql(name, new JsonObject());
	}

	private GraphQLResponse graphql(String name, String key, String value) throws IOException {
		return graphql(name, new JsonObject().put(key, value));
	}

	private GraphQLResponse graphql(String name, JsonObject variables) throws IOException {
		String queryName = "emptyNode/" + name;
		GraphQLRequest request = new GraphQLRequest()
			.setQuery(getGraphQLQuery(queryName))
			.setVariables(variables);
		GraphQLResponse graphQLResponse = client().graphql(PROJECT_NAME, request).blockingGet();
		JsonObject json = new JsonObject(graphQLResponse.toJson());
		assertThat(json).compliesToAssertions(queryName);

		return graphQLResponse;
	}
}
