package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeImageResizeEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testImageResize() throws Exception {
		// 1. Upload image
		uploadImage(tx(() -> folder("news")), "en", "image");

		try (Tx tx = tx()) {
			HibNode node = folder("news");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
			String etag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params));

			callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false, 304);

			params.setHeight(103);
			String newETag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false, 200);
			callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), newETag, false, 304);

		}
	}
}
