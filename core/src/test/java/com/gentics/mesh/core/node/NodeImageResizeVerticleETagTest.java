package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class NodeImageResizeVerticleETagTest extends AbstractETagTest {

	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testImageResize() throws Exception {

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(100).setHeight(102);
			MeshResponse<NodeDownloadResponse> response = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params)
					.invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));

			expect304(getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false);

			params.setHeight(103);
			String newETag = expectNo304(getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), etag, false);
			expect304(getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), newETag, false);

		}
	}
}
