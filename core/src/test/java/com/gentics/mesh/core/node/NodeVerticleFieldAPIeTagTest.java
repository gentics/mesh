package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class NodeVerticleFieldAPIeTagTest extends AbstractETagTest {

	private NodeVerticle verticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadOne() throws Exception {

		// 1. Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "node_binary_field_updated", "binary");

			node.reload();

			// 2. Download the data using the field api
			MeshResponse<NodeDownloadResponse> response = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary").invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull("A etag should have been generated.", etag);
			expect304(getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary"), etag, false);
		}

	}

}
