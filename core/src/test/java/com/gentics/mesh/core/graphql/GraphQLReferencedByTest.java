package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.util.TestUtils.getResourceAsString;
import static java.util.Collections.singletonMap;
import static java.util.Objects.hash;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLReferencedByTest extends AbstractMeshTest {

	@Test
	public void testAllReferences() throws IOException {
		createSchema();
		NodeResponse targetNode = readNode(projectName(), tx(() -> folder("2015").getUuid()));

		NodeResponse node = createReferenceNode(targetNode.getUuid());
		GraphQLResponse response = graphQl(
			getGraphQLQuery("referencedBy/query"),
			singletonMap("uuid", targetNode.getUuid())
		);

		JsonArray responseData = response.getData()
			.getJsonObject("node")
			.getJsonObject("referencedBy")
			.getJsonArray("elements");

		String sourceUuid = node.getUuid();
		assertThat(responseData).containsJsonObjectHashesInAnyOrder(
			obj -> hash(
				obj.getString("fieldName"),
				obj.getString("micronodeFieldName"),
				obj.getJsonObject("node").getString("uuid")
			),
			hash("simpleNodeRef", null, sourceUuid),
			hash("listNodeRef", null, sourceUuid),
			hash("simpleMicronode", "microSimpleNodeRef", sourceUuid),
			hash("simpleMicronode", "microListNodeRef", sourceUuid),
			hash("listMicronode", "microSimpleNodeRef", sourceUuid),
			hash("listMicronode", "microListNodeRef", sourceUuid)
		);
	}

	private GraphQLResponse graphQl(String query, Map<String, Object> variables) {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(query);
		request.setVariables(new JsonObject(variables));
		return client().graphql(PROJECT_NAME, request).blockingGet();
	}

	/**
	 * Creates a schema which contains all possible node reference fields.
	 * @return
	 */
	private SchemaResponse createSchema() {
		String projectName = projectName();

		SchemaCreateRequest schemaRequest = JsonUtil.readValue(getResourceAsString("/graphql/referencedBy/schema.json"), SchemaCreateRequest.class);
		MicroschemaCreateRequest microschemaRequest = JsonUtil.readValue(getResourceAsString("/graphql/referencedBy/microschema.json"), MicroschemaCreateRequest.class);


		return Single.zip(
			client().createSchema(schemaRequest).toSingle()
				.flatMap(schemaResponse ->
				client().assignSchemaToProject(projectName, schemaResponse.getUuid()).toSingle()),
			client().createMicroschema(microschemaRequest).toSingle()
				.flatMap(microschemaResponse ->
				client().assignMicroschemaToProject(projectName, microschemaResponse.getUuid()).toSingle()),
			(schema, ignore) -> schema
		).blockingGet();
	}

	private NodeResponse createReferenceNode(String uuid) {
		NodeCreateRequest request = JsonUtil.readValue(
			getResourceAsString("/graphql/referencedBy/refNode.json").replaceAll("%UUID%", uuid),
			NodeCreateRequest.class
		);

		return client().createNode(projectName(), request).blockingGet();
	}
}
