package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

public class NodeEndpointFieldAPIeTagTest extends AbstractETagTest {

	@Test
	public void testReadOne() throws Exception {

		// 1. Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			node.reload();

			// 2. Download the data using the field api
			MeshResponse<NodeDownloadResponse> response = client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary").invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull("A etag should have been generated.", etag);
			expect304(client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary"), etag, false);
		}

	}

}
