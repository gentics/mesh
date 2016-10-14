package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class NodeEndpointFieldAPITest extends AbstractRestEndpointTest {

	@Test
	public void testDownloadBinaryField() throws IOException {

		// 1. Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			GenericMessageResponse message = call(() -> uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName));
			expectResponseMessage(message, "node_binary_field_updated", "binary");

			node.reload();

			// 2. Download the data using the field api
			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary")
					.invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);
			assertEquals(binaryLen, downloadFuture.result().getBuffer().length());
		}
	}
}
