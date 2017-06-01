package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeImageResizeEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testImageResize() throws Exception {

		try (Tx tx = db().tx()) {
			Node node = folder("news");

			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
			MeshResponse<NodeDownloadResponse> response = client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params).invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getRawResponse().getHeader(ETAG));

			callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false, 304);

			params.setHeight(103);
			String newETag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false, 200);
			callETag(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), newETag, false, 304);

		}
	}
}
