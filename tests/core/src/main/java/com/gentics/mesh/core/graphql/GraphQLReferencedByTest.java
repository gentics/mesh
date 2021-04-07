package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.role.RolePermissionRequest.withPermissions;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.util.TestUtils.getResourceAsString;
import static java.util.Collections.singletonMap;
import static java.util.Objects.hash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.Rug;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLReferencedByTest extends AbstractMeshTest {

	private NodeResponse sourceNode;
	private NodeResponse targetNode;

	@Before
	public void setUp() throws Exception {
		createSchema();
		targetNode = readNode(projectName(), tx(() -> folder("2015").getUuid()));
		sourceNode = createReferenceNode(targetNode.getUuid());
	}

	@Test
	public void testAllReferences() throws IOException {
		JsonArray responseData = query("basic-query");

		String sourceUuid = sourceNode.getUuid();
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

	@Test
	public void testPagedReferences() throws IOException {
		JsonArray responseData = query("paged-query");
		assertEquals("The array should only contain two elements.", 2, responseData.size());
	}

	@Test
	public void testPermissions() throws IOException {
		Rug tester = createUserGroupRole("tester");

		client().updateRolePermissions(
			tester.getRole().getUuid(),
			String.format("/projects/%s/nodes/%s", projectUuid(), targetNode.getUuid()),
			withPermissions(READ)
		).blockingAwait();

		client().setLogin(tester.getUser().getUsername(), "test1234");
		client().login().blockingGet();

		JsonArray resultData = query("basic-query");
		assertThat(resultData.getList()).isEmpty();
	}

	private JsonArray query(String queryCaseName) throws IOException {
		String queryName = "referencedBy/" + queryCaseName;

		GraphQLResponse response = graphQl(
			getGraphQLQuery(queryName),
			singletonMap("uuid", targetNode.getUuid())
		);
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);

		return response.getData()
			.getJsonObject("node")
			.getJsonObject("referencedBy")
			.getJsonArray("elements");
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
