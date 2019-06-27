package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.plugin.ClonePlugin;
import com.gentics.mesh.plugin.PluginManager;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.ServiceHelper;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPluginTest extends AbstractMeshTest {

	private static PluginManager manager = ServiceHelper.loadFactory(PluginManager.class);

	@Test
	public void testGraphQL() throws IOException {

		grantAdminRole();

		String uuid = "261f779ff7954d0ca60c1f10c6434f28";
		ClonePlugin first = new ClonePlugin(null);
		first.getManifest();
		first.setUuid(uuid);
		manager.getPlugins().put(uuid, first);

		final String CLONE_PLUGIN_DEPLOYMENT_NAME = ClonePlugin.class.getCanonicalName();
		for (int i = 0; i < 100; i++) {
			call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(CLONE_PLUGIN_DEPLOYMENT_NAME)));
		}

		String queryName = "plugin-query";
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		System.out.println(response.toJson());
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);

	}
}
