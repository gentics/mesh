package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeImageResizeVerticleTest extends AbstractBinaryVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Test
	public void testImageResize() throws Exception {

		// 1. Upload image
		String contentType = "image/jpeg";
		String fileName = "blume.jpg";
		Node node = folder("news");
		prepareSchema(node, true, "image/.*");
		resetClientSchemaStorage();

		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		Future<GenericMessageResponse> future = getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), buffer, fileName, contentType);
		latchFor(future);
		assertSuccess(future);

		// 2. Resize image
		Future<NodeDownloadResponse> downloadResponse = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(),
				new ImageRequestParameter().setWidth(100).setHeight(100));
		latchFor(downloadResponse);
		NodeDownloadResponse download = downloadResponse.result();
		assertNotNull(download);
		CountDownLatch latch = new CountDownLatch(1);
		Mesh.vertx().fileSystem().writeFile("target/resized.jpg", download.getBuffer(), rh -> {
			assertTrue(rh.succeeded());
			latch.countDown();
		});
		failingLatch(latch);
	}

}