package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.DummyPlugin;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true, elasticsearch = ElasticsearchTestMode.CONTAINER_ES6)
public class MetricsLabelTest extends AbstractMeshTest {

	private final String esQuery = new JsonObject()
		.put("query", new JsonObject()
			.put("match_all", new JsonObject())).toString();

	@Test
	public void testGraphQL() {
		String query = "{ me { uuid } }";
		testMetric(pathLabel("graphql"), client().graphqlQuery(PROJECT_NAME, query));
	}

	@Test
	public void testWebroot() {
		testMetric(pathLabel("webroot"), client().webroot(PROJECT_NAME, "/"));
	}

	@Test
	public void testNodes() {
		testMetric(pathLabel("nodes"), client().findNodes(PROJECT_NAME));
	}

	@Test
	public void testSingleNode() {
		NodeResponse node = client().findNodes(PROJECT_NAME).blockingGet().getData().get(0);
		testMetric(pathLabel("nodes"), client().findNodeByUuid(PROJECT_NAME, node.getUuid()));
	}

	@Test
	public void testBinaries() throws IOException {
		NodeResponse binaryNode = createBinaryContent().blockingGet();
		uploadImage(binaryNode);
		testMetric(pathLabel("binary"), client().downloadBinaryField(PROJECT_NAME, binaryNode.getUuid(), binaryNode.getLanguage(), "binary"));
	}

	@Test
	public void testSearch() {
		testMetric(pathLabel("search"), client().searchNodes(esQuery));
	}

	@Test
	public void testSearchProject() {
		testMetric(pathLabel("search"), client().searchNodes(PROJECT_NAME, esQuery));
	}

	@Test
	public void testPlugin() {
		meshApi2().deployPlugin(DummyPlugin.class, "dummy").blockingAwait();
		waitForPreRegistration();
		testMetric(pathLabel("plugin_dummy"), client().get("/plugins/dummy/hello"));
	}

	@Test
	public void testProjectPlugin() {
		meshApi2().deployPlugin(DummyPlugin.class, "dummy").blockingAwait();
		waitForPreRegistration();
		testMetric(pathLabel("plugin_dummy"), client().get("/dummy/plugins/dummy/hello"));
	}

	@After
	public void tearDown() throws Exception {
		pluginManager().unloadPlugins();
	}

	private String pathLabel(String label) {
		return "path=\"" + label + "\"";
	}

	private void testMetric(String expected, MeshRequest<?> loader) {
		for (int i = 0; i < 10; i++) {
			loader.blockingAwait();
		}
		String metrics = call(() -> monClient().metrics());
		assertThat(metrics).as("Metrics result").isNotEmpty().contains(expected);
	}
}
