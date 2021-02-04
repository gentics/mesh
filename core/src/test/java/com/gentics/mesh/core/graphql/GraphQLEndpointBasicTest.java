package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
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
			for (Microschema microschema : meshRoot().getMicroschemaContainerRoot().findAll()) {
				microschema.remove();
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
}
