package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.VersionNumber;
import com.gentics.madl.tx.Tx;

import io.vertx.core.buffer.Buffer;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeImageResizeEndpointTest extends AbstractMeshTest {

	@Test
	public void testImageResize() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());
		try (Tx tx = tx()) {
			Node node = folder("news");

			// 1. Upload image
			uploadImage(node, "en", "image");
		}

		// 2. Resize image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
		NodeDownloadResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image", params));

		// 3. Validate resize
		try (Tx tx = tx()) {
			Node node = folder("news");
			validateResizeImage(download, node.getLatestDraftFieldContainer(english()).getBinary("image"), params, 100, 102);
		}

	}

	@Test
	public void testImageResizeOverLimit() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(options.getMaxWidth() + 1).setHeight(102);
			call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params), BAD_REQUEST,
				"image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(options.getMaxWidth() + 1));
		}
	}

	@Test
	public void testImageExactLimit() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			ImageManipulatorOptions options = Mesh.mesh().getOptions().getImageOptions();
			// 1. Upload image
			uploadImage(node, "en", "image");

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(options.getMaxWidth()).setHeight(102);
			NodeDownloadResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params));

			assertNotNull(node.getLatestDraftFieldContainer(english()));
			validateResizeImage(download, node.getLatestDraftFieldContainer(english()).getBinary("image"), params, 2048, 102);
		}
	}

	@Test
	public void testTransformImage() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());
		String version = db().tx(() -> {
			Node node = folder("news");
			// 1. Upload image
			return uploadImage(node, "en", "image").getVersion();
		});

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		NodeDownloadResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 100, 118);

	}

	@Test
	public void testTransformImageCrop() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());
		String version = db().tx(() -> {
			Node node = folder("news");
			// 1. Upload image
			return uploadImage(node, "en", "image").getVersion();
		});

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth(100);
		params.setHeight(200);
		params.setRect(0, 0, 200, 200);

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		NodeDownloadResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 100, 200);

	}

	@Test
	public void testTransformImageNoParameters() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			// 1. Upload image
			NodeResponse response = uploadImage(node, "en", "image");

			// 2. Transform the image
			ImageManipulationParametersImpl params = new ImageManipulationParametersImpl();
			call(() -> client().transformNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", response.getVersion(), "image", params));
		}
	}

	@Test
	public void testTransformNonBinary() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());
		String version = db().tx(() -> {
			Node node = folder("news");
			return node.getGraphFieldContainer("en").getVersion().toString();
		});

		// try to transform the "name"
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);

		call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "name", params), BAD_REQUEST,
			"error_found_field_is_not_binary", "name");
	}

	@Test
	public void testTransformNonImage() throws Exception {
		VersionNumber version;
		String nodeUuid;
		try (Tx tx = tx()) {
			Node node = folder("news");
			nodeUuid = node.getUuid();
			prepareSchema(node, "*/*", "image");
			version = node.getGraphFieldContainer("en").getVersion();
			tx.success();
		}
		NodeResponse response = call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version.toString(), "image", Buffer.buffer(
			"I am not an image"), "test.txt", "text/plain"));

		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", response.getVersion(), "image", params), BAD_REQUEST,
			"error_transformation_non_image", "image");
	}

	@Test
	public void testResizeWithFocalPoint() throws IOException {
		try (Tx tx = tx()) {
			Node node = folder("news");
			// 1. Upload image
			NodeResponse response = uploadImage(node, "en", "image");

			// 2. Update the binary field and set the focal point
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			BinaryField imageField = response.getFields().getBinaryField("image");
			imageField.setFocalPoint(0.1f, 0.2f);
			updateRequest.setLanguage("en");
			updateRequest.setVersion(response.getVersion());
			updateRequest.getFields().put("image", imageField);
			NodeResponse response2 = call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), updateRequest));
			assertEquals(0.1f, response2.getFields().getBinaryField("image").getFocalPoint().getX(), 0);
			assertEquals(0.2f, response2.getFields().getBinaryField("image").getFocalPoint().getY(), 0);

			// 2. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(600).setHeight(102);
			call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "image", params));
		}
	}
	
	@Test
	public void testFocalPointZoomWithTooLargeTarget() throws IOException {
		String uuid;
		try (Tx tx = tx()) {
			Node node = folder("news");
			// 1. Upload image
			NodeResponse response = uploadImage(node, "en", "image");
			uuid = response.getUuid();
		}
			
		// 2. Zoom into image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(2048).setHeight(2048);
		params.setFocalPoint(0.5f, 0.5f);
		params.setFocalPointZoom(1.5f);
		params.setCropMode(CropMode.FOCALPOINT);
		call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image", params), BAD_REQUEST,
				"image_error_target_too_large_for_zoom");
	}

	@Test
	public void testResizeWithFocalPointOutOfBounds() throws IOException {
		try (Tx tx = tx()) {
			Node node = folder("news");
			// 1. Upload image
			NodeResponse response = uploadImage(node, "en", "image");

			// 2. Update the binary field and set the focal point
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			BinaryField imageField = response.getFields().getBinaryField("image");
			imageField.setFocalPoint(2.5f, 2.21f);
			updateRequest.setLanguage("en");
			updateRequest.setVersion(response.getVersion());
			updateRequest.getFields().put("image", imageField);
			call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), updateRequest), BAD_REQUEST,
					"field_binary_error_image_focalpoint_out_of_bounds", "image", "2.5-2.21", "1376:1160");

      // No try the exact x bounds
			imageField.setFocalPoint(1f, 1f);
			call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), updateRequest));
		}
	}

	@Test
	public void testTransformEmptyField() throws Exception {
		String nodeUuid;
		String version;
		try (Tx tx = tx()) {
			Node node = folder("news");
			nodeUuid = node.getUuid();
			prepareSchema(node, "image/.*", "image");
			version = node.getGraphFieldContainer("en").getVersion().toString();
			tx.success();
		}
		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version, "image", params), NOT_FOUND,
			"error_binaryfield_not_found_with_name", "image");

	}

	@Test
	public void testTransformImageFilename() throws Exception {
		String uuid = db().tx(() -> folder("news").getUuid());

		String version = db().tx(() -> {
			Node node = folder("news");
			// 1. Upload image
			return uploadImage(node, "en", "image").getVersion();
		});

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		NodeDownloadResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the filename
		assertEquals("blume.jpg", result.getFilename());
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
			File cacheFile = meshDagger().imageManipulator().getCacheFile(binaryField.getBinary().getSHA512Sum(), params);
			assertTrue("The cache file could not be found in the cache directory. {" + cacheFile.getAbsolutePath() + "}", cacheFile.exists());
		}
	}

}