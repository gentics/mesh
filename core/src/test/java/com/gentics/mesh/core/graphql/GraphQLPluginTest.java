package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

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
		grantAdminRole();

		for (int i = 0; i < 100; i++) {
			deployPlugin(ClonePlugin.class, "clone" + i);
		}

		String queryName = "plugin-query";
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		//System.out.println(response.toJson());
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);
	}
}
