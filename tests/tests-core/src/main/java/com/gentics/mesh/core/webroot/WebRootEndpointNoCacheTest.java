package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.MeshOptionChanger.NO_PATH_CACHE;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = NO_PATH_CACHE)
public class WebRootEndpointNoCacheTest extends AbstractMeshTest {

	@Test
	public void testReadBinaryNode() throws IOException {
		String path = "/News/2015/News_2015.en.html";

		for (int i = 0; i < 10; i++) {
			MeshWebrootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")));
			try (Tx tx = tx()) {
				HibNode node = content("news_2015");
				assertThat(restNode.getNodeResponse()).is(node).hasLanguage("en");
			}
		}
	}
}
