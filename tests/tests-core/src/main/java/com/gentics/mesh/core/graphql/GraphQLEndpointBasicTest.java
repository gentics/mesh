package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.MeshJSONAssert;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointBasicTest extends AbstractMeshTest {

	@Test
	public void testIntrospection() throws IOException {
		try (Tx tx = tx()) {
			for (Microschema microschema : tx.microschemaDao().findAll()) {
				tx.microschemaDao().delete(microschema, new DummyBulkActionContext());
			}
			tx.success();
		}
		String queryName = "introspection-query";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json).compliesToAssertions(queryName);
	}

	@Test
	public void testSimpleQuery() throws JSONException {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'me':{'firstname':'Joe'}}", response.getData());
	}

	@Test
	public void testEmptyQuery() throws Throwable {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, ""));
		assertEquals(1, response.getErrors().stream().filter(error -> error.getType().equals("InvalidSyntax")).count());
	}

	@Test
	public void testErrorHandling() throws Throwable {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, "{bogus{firstname}}"));
		assertEquals(1, response.getErrors().stream().filter(error -> error.getType().equals("ValidationError")).count());
	}

	@Test
	public void testVariables() throws Throwable {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery("query test($var: String) { node(path: $var) { node { uuid } } }");
		request.setVariables(new JsonObject().put("var", "/News"));
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		String uuid = response.getData().getJsonObject("node").getJsonObject("node").getString("uuid");
		assertThat(uuid).isNotEmpty();
	}

	@Test
	public void testConcurrentQuery() {
		Flowable<Completable> calls = Single.fromCallable(() ->
			client().graphqlQuery(PROJECT_NAME, "{me{firstname}}").toSingle()
				.doOnSuccess(response -> MeshJSONAssert.assertEquals("{'me':{'firstname':'Joe'}}", response.getData()))
				.toCompletable())
			.repeat(100);

		Completable.merge(calls).blockingAwait();
	}

	/**
	 * Test getting schemas from a project that has a schema without fields assigned (API v1)
	 * @throws JSONException
	 */
	@Test
	public void testEmptySchemaQueryV1() throws JSONException {
		doEmptySchemaQueryTest("v1");
	}

	/**
	 * Test getting schemas from a project that has a schema without fields assigned (API v2)
	 * @throws JSONException
	 */
	@Test
	public void testEmptySchemaQueryV2() throws JSONException {
		doEmptySchemaQueryTest("v2");
	}

	/**
	 * Do the test for getting schemas via graphql where a schema has no fields
	 * @param version API version
	 * @throws JSONException
	 */
	protected void doEmptySchemaQueryTest(String version) throws JSONException {
		SchemaCreateRequest request = new SchemaCreateRequest().setName("emptyschema");
		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		assertTrue(schemaResponse.getFields().isEmpty());
		GraphQLResponse response = call(() -> client(version).graphqlQuery(PROJECT_NAME, "{empty: schema(name:\"emptyschema\") {name fields{name}}}"));
		MeshJSONAssert.assertEquals("{'empty': {'name': 'emptyschema','fields': []}}", response.getData());
	}

	/**
	 * Test getting microschemas from a project that has a microschema without fields assigned (API v1)
	 * @throws JSONException
	 */
	@Test
	public void testEmptyMicroschemaQueryV1() throws JSONException {
		doEmptyMicroschemaQueryTest("v1");
	}

	/**
	 * Test getting microschemas from a project that has a microschema without fields assigned (API v2)
	 * @throws JSONException
	 */
	@Test
	public void testEmptyMicroschemaQueryV2() throws JSONException {
		doEmptyMicroschemaQueryTest("v2");
	}

	/**
	 * Do the test for getting microschemas via graphql where a microschema has no fields
	 * @param version API version
	 * @throws JSONException
	 */
	protected void doEmptyMicroschemaQueryTest(String version) throws JSONException {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest().setName("emptymicroschema");
		MicroschemaResponse schemaResponse = call(() -> client().createMicroschema(request));
		assertTrue(schemaResponse.getFields().isEmpty());
		GraphQLResponse response = call(() -> client(version).graphqlQuery(PROJECT_NAME, "{empty: microschema(name:\"emptymicroschema\") {name fields{name}}}"));
		MeshJSONAssert.assertEquals("{'empty': {'name': 'emptymicroschema','fields': []}}", response.getData());
	}

	/**
	 * Test getting schemas from a project, which has no schemas assigned (API v1)
	 */
	@Test
	public void testNoSchemasV1() {
		doNoSchemasTest("v1");
	}

	/**
	 * Test getting schemas from a project, which has no schemas assigned (API v2)
	 */
	@Test
	public void testNoSchemasV2() {
		doNoSchemasTest("v1");
	}

	/**
	 * Do the test for getting schemas from a project without schemas
	 * @param version API version
	 */
	protected void doNoSchemasTest(String version) {
		// create a project
		String projectName = "no_schemas";
		call(() -> client().createProject(new ProjectCreateRequest().setName(projectName).setSchemaRef("folder")));

		// unassign all schemas from the project
		SchemaListResponse schemas = call(() -> client().findSchemas(projectName));
		schemas.getData().forEach(schema -> {
			call(() -> client().unassignSchemaFromProject(projectName, schema.getUuid()));
		});

		// perform a graphql request
		call(() -> client(version).graphqlQuery(projectName, "{ schemas { elements { name } } }"));
	}
}
