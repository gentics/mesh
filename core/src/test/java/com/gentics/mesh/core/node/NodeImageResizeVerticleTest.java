package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeImageResizeVerticleTest extends AbstractBinaryVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Test
	public void testImageResize() throws Exception {

		// 1. Upload image
		String contentType = "image/jpeg";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		prepareSchema(node, true, "image/.*");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

		// 2. Resize image
		Future<NodeDownloadResponse> downloadResponse = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(),
				new ImageRequestParameter().setWidth(100).setHeight(100));
		latchFor(downloadResponse);
		NodeDownloadResponse download = downloadResponse.result();
		assertNotNull(download);
	}

}