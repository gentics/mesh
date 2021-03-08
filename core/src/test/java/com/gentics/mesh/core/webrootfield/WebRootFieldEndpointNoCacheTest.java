package com.gentics.mesh.core.webrootfield;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshOptionChanger.NO_PATH_CACHE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = NO_PATH_CACHE)
public class WebRootFieldEndpointNoCacheTest extends AbstractMeshTest {

	@Test
	public void testReadBinaryNode() throws IOException {
		String path = "/News/2015/News_2015.en.html";

		for (int i = 0; i < 10; i++) {
			MeshWebrootFieldResponse restNode = call(() -> client().webrootField(PROJECT_NAME, "content", path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")));
			try (Tx tx = tx()) {
				assertEquals(restNode.getResponseAsPlainText(), "News!");
			}
		}
	}
}
