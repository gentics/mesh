package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.ImageManipulationParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.buffer.Buffer;

public class NodeImageResizeVerticleTest extends AbstractIsolatedRestVerticleTest {

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
			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params)
					.invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);

			// 3. Validate resize
			node.reload();
			validateResizeImage(downloadFuture.result(), node.getLatestDraftFieldContainer(english()).getBinary("image"), params, 100, 102);
		}
	}

	@Test
	public void testImageResizeOverLimit() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(options.getMaxWidth() + 1).setHeight(102);
			call(() -> getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), BAD_REQUEST,
					"image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(options.getMaxWidth() + 1));
		}
	}

	@Test
	public void testImageExactLimit() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(options.getMaxWidth()).setHeight(102);
			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params)
					.invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);
			node.reload();
			assertNotNull(node.getLatestDraftFieldContainer(english()));
			validateResizeImage(downloadFuture.result(), node.getLatestDraftFieldContainer(english()).getBinary("image"), params, 2048, 102);
		}
	}

	@Test
	public void testTransformImage() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Transform the image
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(100);
			MeshResponse<GenericMessageResponse> transformFuture = getClient()
					.transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params).invoke();
			latchFor(transformFuture);
			assertSuccess(transformFuture);

			// 3. Download the image
			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image").invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);

			// 4. Validate the resized image
			validateResizeImage(downloadFuture.result(), null, params, 100, 118);
		}
	}

	@Test
	public void testTransformImageNoParameters() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Transform the image
			ImageManipulationParameters params = new ImageManipulationParameters();
			MeshResponse<GenericMessageResponse> transformFuture = getClient()
					.transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params).invoke();
			latchFor(transformFuture);
			expectException(transformFuture, BAD_REQUEST, "error_no_image_transformation", "image");
		}
	}

	@Test
	public void testTransformNonBinary() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			// try to transform the "name"
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(100);
			MeshResponse<GenericMessageResponse> transformFuture = getClient()
					.transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "name", params).invoke();
			latchFor(transformFuture);
			expectException(transformFuture, BAD_REQUEST, "error_found_field_is_not_binary", "name");
		}
	}

	@Test
	public void testTransformNonImage() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			prepareSchema(node, "*/*", "image");

			// upload non-image data
			MeshResponse<GenericMessageResponse> uploadFuture = getClient()
					.updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", Buffer.buffer("I am not an image"), "test.txt", "text/plain")
					.invoke();
			latchFor(uploadFuture);
			assertSuccess(uploadFuture);

			// Transform
			ImageManipulationParameters params = new ImageManipulationParameters().setWidth(100);
			MeshResponse<GenericMessageResponse> transformFuture = getClient()
					.transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params).invoke();
			latchFor(transformFuture);
			expectException(transformFuture, BAD_REQUEST, "error_transformation_non_image", "image");
		}
	}

	@Test
	public void testTransformEmptyField() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			prepareSchema(node, "image/.*", "image");

			// 2. Transform the image
			ImageManipulationParameters params = new ImageManipulationParameters();
			call(() -> getClient().transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), NOT_FOUND,
					"error_binaryfield_not_found_with_name", "image");
		}
	}

	private void validateResizeImage(NodeDownloadResponse download, BinaryGraphField binaryField, ImageManipulationParameters params,
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

}