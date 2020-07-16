package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.plugin.AbstractPluginTest;
import com.gentics.mesh.plugin.ClonePlugin;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPluginTest extends AbstractPluginTest {

	@Test
	public void testGraphQL() throws IOException {
		grantAdmin();

		for (int i = 1; i <= 100; i++) {
			deployPlugin(ClonePlugin.class, "clone" + i);
		}

		waitForPluginRegistration();
		String queryName = "plugin/plugin-query";
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		// System.out.println(response.toJson());
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);
	}

	@Test
	public void testGraphQLPlugin() throws IOException {
		grantAdmin();

		copyAndDeploy(GRAPHQL_PATH, "graphql.jar");
		waitForPluginRegistration();

		String queryName = "plugin/graphql-plugin-query";
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);
	}

	@Test
	public void testInvalidGraphQLPlugin() throws IOException {
		grantAdmin();

		copyAndDeploy(INVALID_GRAPHQL_PATH, "graphql.jar", BAD_REQUEST, "admin_plugin_error_invalid_gql_name", "invalid-plugin");

	}
}
