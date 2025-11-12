package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.DummyPlugin;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true, elasticsearch = ElasticsearchTestMode.CONTAINER_ES6)
public class MetricsLabelTest extends AbstractMeshTest {

	private final String esQuery = new JsonObject()
		.put("query", new JsonObject()
			.put("match_all", new JsonObject())).toString();

	@Test
	public void testGraphQL() {
		String query = "{ me { uuid } }";
		testMetric(pathLabel(PROJECT_NAME + "/graphql"), client().graphqlQuery(PROJECT_NAME, query));
	}

	@Test
	public void testWebroot() {
		testMetric(pathLabel(PROJECT_NAME + "/webroot/"), client().webroot(PROJECT_NAME, "/"));
	}

	@Test
	public void testWebrootField() throws IOException {
		NodeResponse binaryNode = createBinaryContent().blockingGet();
		binaryNode = uploadImage(binaryNode);
		binaryNode = client().findNodeByUuid(PROJECT_NAME, binaryNode.getUuid(), new NodeParametersImpl().setResolveLinks(LinkType.SHORT)).blockingGet();
		testMetric(pathLabel(PROJECT_NAME + "/webrootfield/binary/blume.jpg"), client().webrootField(PROJECT_NAME, "binary", binaryNode.getPath()));
	}

	@Test
	public void testNodes() {
		testMetric(pathLabel(PROJECT_NAME + "/nodes"), client().findNodes(PROJECT_NAME));
	}

	@Test
	public void testSingleNode() {
		NodeResponse node = client().findNodes(PROJECT_NAME).blockingGet().getData().get(0);
		testMetric(pathLabel(PROJECT_NAME + "/nodes/" + node.getUuid()), client().findNodeByUuid(PROJECT_NAME, node.getUuid()));
	}

	@Test
	public void testBinaries() throws IOException {
		NodeResponse binaryNode = createBinaryContent().blockingGet();
		uploadImage(binaryNode);
		testMetric(pathLabel(PROJECT_NAME + "/nodes/" + binaryNode.getUuid() + "/binary/binary"), client().downloadBinaryField(PROJECT_NAME, binaryNode.getUuid(), binaryNode.getLanguage(), "binary"));
	}

	@Test
	public void testSearch() {
		testMetric(pathLabel("search/nodes"), client().searchNodes(esQuery));
	}

	@Test
	public void testSearchProject() {
		testMetric(pathLabel(PROJECT_NAME + "/search/nodes"), client().searchNodes(PROJECT_NAME, esQuery));
	}

	@Test
	public void testPlugin() {
		meshApi2().deployPlugin(DummyPlugin.class, "dummy").blockingAwait();
		waitForPluginRegistration();
		testMetric(pathLabel("plugins/dummy/hello"), client().get("/plugins/dummy/hello"));
	}

	@Test
	public void testProjectPlugin() {
		meshApi2().deployPlugin(DummyPlugin.class, "dummy").blockingAwait();
		waitForPluginRegistration();
		testMetric(pathLabel(PROJECT_NAME + "/plugins/dummy/hello"), client().get("/dummy/plugins/dummy/hello"));
	}

	@After
	public void tearDown() throws Exception {
		pluginManager().unloadPlugins();
	}

	private String pathLabel(String label) {
		return "path=\"/api/v2/" + label + "\"";
	}

	private void testMetric(String expected, MeshRequest<?> loader) {
		for (int i = 0; i < 10; i++) {
			loader.blockingAwait();
		}
		String metrics = call(() -> monClient().metrics());
		assertThat(metrics).as("Metrics result").isNotEmpty().contains(expected);
	}
}
