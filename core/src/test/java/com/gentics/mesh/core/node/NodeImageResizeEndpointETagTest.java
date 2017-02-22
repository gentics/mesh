package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeImageResizeEndpointETagTest extends AbstractETagTest {

	@Test
	public void testImageResize() throws Exception {

		try (NoTx noTrx = db().noTx()) {
			Node node = folder("news");

			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(100).setHeight(102);
			MeshResponse<NodeDownloadResponse> response = client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params).invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));

			expect304(client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false);

			params.setHeight(103);
			String newETag = expectNo304(client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false);
			expect304(client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), newETag, false);

		}
	}
}
