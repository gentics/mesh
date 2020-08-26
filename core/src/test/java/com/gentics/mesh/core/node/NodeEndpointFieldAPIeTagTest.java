package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeEndpointFieldAPIeTagTest extends AbstractMeshTest {

	@Test
	public void testReadOne() throws Exception {

		// 1. Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		HibNode node = folder("news");
		String nodeUuid = node.getUuid();

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

		// 2. Download the data using the field api
		String etag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"));
		assertNotNull("A etag should have been generated.", etag);
		callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"), etag, false, 304);
	}

	@Test
	public void testChangedBinary() throws Exception {
		// 1. Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		HibNode node = folder("news");
		String nodeUuid = node.getUuid();

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

		// 2. Download the data using the field api
		String etag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"));
		assertNotNull("A etag should have been generated.", etag);
		callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"), etag, false, 304);

		// 3. Change the binary data and try again
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		etag = callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"), etag, false, 200);
		assertNotNull("A etag should have been generated.", etag);
		callETag(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "binary"), etag, false, 304);
	}
}
