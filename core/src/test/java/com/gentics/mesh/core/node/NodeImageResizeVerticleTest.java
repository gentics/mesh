package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.query.impl.ImageManipulationParameter;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeImageResizeVerticleTest extends AbstractBinaryVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testImageResize() throws Exception {
		Node node = folder("news");

		// 1. Upload image
		uploadImage(node, "en", "image");

		// 2. Resize image
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(100).setHeight(102);
		Future<NodeDownloadResponse> downloadFuture = resizeImage(node, params);
		latchFor(downloadFuture);
		assertSuccess(downloadFuture);

		// 3. Validate resize
		validateResizeImage(downloadFuture.result(), node.getGraphFieldContainer(english()).getBinary("image"), params, 100, 102);
	}

	@Test
	public void testImageResizeOverLimit() throws Exception {
		Node node = folder("news");
		ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
		// 1. Upload image
		uploadImage(node, "en", "image");

		// 2. Resize image
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(options.getMaxWidth() + 1).setHeight(102);
		Future<NodeDownloadResponse> download = resizeImage(node, params);
		latchFor(download);
		expectException(download, BAD_REQUEST, "image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()),
				String.valueOf(options.getMaxWidth() + 1));

	}

	@Test
	public void testImageExactLimit() throws Exception {
		Node node = folder("news");
		ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
		// 1. Upload image
		uploadImage(node, "en", "image");

		// 2. Resize image
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(options.getMaxWidth()).setHeight(102);
		Future<NodeDownloadResponse> downloadFuture = resizeImage(node, params);
		latchFor(downloadFuture);
		assertSuccess(downloadFuture);
		validateResizeImage(downloadFuture.result(), node.getGraphFieldContainer(english()).getBinary("image"), params, 2048, 102);

	}

	@Test
	public void testTransformImage() throws Exception {
		Node node = folder("news");
		// 1. Upload image
		uploadImage(node, "en", "image");

		// 2. Transform the image
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(100);
		Future<GenericMessageResponse> transformFuture = getClient().transformNodeBinaryField(PROJECT_NAME,
				node.getUuid(), "en", "image", params);
		latchFor(transformFuture);
		assertSuccess(transformFuture);

		// 3. Download the image
		Future<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image");
		latchFor(downloadFuture);
		assertSuccess(downloadFuture);

		// 4. Validate the resized image
		validateResizeImage(downloadFuture.result(), null, params, 100, 118);
	}

	@Test
	public void testTransformImageNoParameters() throws Exception {
		Node node = folder("news");
		// 1. Upload image
		uploadImage(node, "en", "image");

		// 2. Transform the image
		ImageManipulationParameter params = new ImageManipulationParameter();
		Future<GenericMessageResponse> transformFuture = getClient().transformNodeBinaryField(PROJECT_NAME,
				node.getUuid(), "en", "image", params);
		latchFor(transformFuture);
		expectException(transformFuture, BAD_REQUEST, "error_no_image_transformation", "image");
	}

	@Test
	public void testTransformNonBinary() throws Exception {
		Node node = folder("news");

		// try to transform the "name"
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(100);
		Future<GenericMessageResponse> transformFuture = getClient().transformNodeBinaryField(PROJECT_NAME,
				node.getUuid(), "en", "name", params);
		latchFor(transformFuture);
		expectException(transformFuture, BAD_REQUEST, "error_found_field_is_not_binary", "name");
	}

	@Test
	public void testTransformNonImage() throws Exception {
		Node node = folder("news");

		prepareSchema(node, "*/*", "image");
		resetClientSchemaStorage();

		// upload non-image data
		Future<GenericMessageResponse> uploadFuture = getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en",
				"image", Buffer.buffer("I am not an image"), "test.txt", "text/plain");
		latchFor(uploadFuture);
		assertSuccess(uploadFuture);

		// Transform
		ImageManipulationParameter params = new ImageManipulationParameter().setWidth(100);
		Future<GenericMessageResponse> transformFuture = getClient().transformNodeBinaryField(PROJECT_NAME,
				node.getUuid(), "en", "image", params);
		latchFor(transformFuture);
		expectException(transformFuture, BAD_REQUEST, "error_transformation_non_image", "image");
	}

	@Test
	public void testTransformEmptyField() throws Exception {
		Node node = folder("news");

		prepareSchema(node, "image/.*", "image");
		resetClientSchemaStorage();

		// 2. Transform the image
		ImageManipulationParameter params = new ImageManipulationParameter();
		Future<GenericMessageResponse> transformFuture = getClient().transformNodeBinaryField(PROJECT_NAME,
				node.getUuid(), "en", "image", params);
		latchFor(transformFuture);
		expectException(transformFuture, NOT_FOUND, "error_binaryfield_not_found_with_name", "image");
	}

	private Future<NodeDownloadResponse> resizeImage(Node node, ImageManipulationParameter params) {
		Future<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params);
		latchFor(downloadFuture);
		return downloadFuture;
	}

	private void validateResizeImage(NodeDownloadResponse download, BinaryGraphField binaryField, ImageManipulationParameter params,
			int expectedWidth, int expectedHeight) throws Exception {
		File targetFile = new File("target", UUID.randomUUID() + "_resized.jpg");
		CountDownLatch latch = new CountDownLatch(1);
		Mesh.vertx().fileSystem().writeFile(targetFile.getAbsolutePath(), download.getBuffer(), rh -> {
			assertTrue(rh.succeeded());
			latch.countDown();
		});
		failingLatch(latch);
		assertThat(targetFile).exists();
		BufferedImage img = ImageIO.read(targetFile);
		assertEquals(expectedWidth, img.getWidth());
		assertEquals(expectedHeight, img.getHeight());

		if (binaryField != null) {
			File cacheFile = springConfig.imageProvider().getCacheFile(binaryField.getSHA512Sum(), params);
			assertTrue("The cache file could not be found in the cache directory. {" + cacheFile.getAbsolutePath() + "}", cacheFile.exists());
		}
	}

	private void uploadImage(Node node, String languageTag, String fieldName) throws IOException {
		String contentType = "image/jpeg";
		String fileName = "blume.jpg";
		prepareSchema(node, "image/.*", fieldName);
		resetClientSchemaStorage();

		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		Future<GenericMessageResponse> future = getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), languageTag, fieldName, buffer,
				fileName, contentType);
		latchFor(future);
		assertSuccess(future);
	}

}