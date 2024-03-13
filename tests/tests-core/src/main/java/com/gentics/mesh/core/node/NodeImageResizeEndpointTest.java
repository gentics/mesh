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
import static org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.image.CacheFileInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.image.ResizeMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.assertj.MeshCoreAssertion;
import com.gentics.mesh.test.context.AbstractMeshTest;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@MeshTestSetting(testSize = FULL, startServer = true)
@RunWith(Parameterized.class)
public class NodeImageResizeEndpointTest extends AbstractMeshTest {

	@Parameters(name = "{index}: filetype={0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(
			new Object[] { "jpeg" },
			new Object[] { "webp" });
	}

	@Parameter
	public String fileType;

	private String extension;

	@Before
	public void setup() {
		switch (fileType) {
		case "jpeg":
			extension = "jpg";

			break;

		case "webp":
			extension = "webp";

			break;
		}
	}

	@Test
	public void testImageResize() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		uploadImageType(node, "en", "image", extension, fileType);

		// 2. Resize image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
		MeshBinaryResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image", params));

		// 3. Validate resize
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			validateResizeImage(download, contentDao.getLatestDraftFieldContainer(node, english()).getBinary("image"), params, 100, 102);
		}
	}

	@Test
	public void testImageResizeOverLimit() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		// 1. Upload image
		uploadImageType(node, "en", "image", extension, fileType);
		ImageManipulatorOptions options = options().getImageOptions();

		// 2. Resize image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(options.getMaxWidth() + 1).setHeight(102);
		call(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "image", params), BAD_REQUEST,
			"image_error_width_limit_exceeded", String.valueOf(options.getMaxWidth()), String.valueOf(options.getMaxWidth() + 1));
	}

	@Test
	public void testImageExactLimit() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		// 1. Upload image
		uploadImageType(node, "en", "image", extension, fileType);
		ImageManipulatorOptions options = options().getImageOptions();

		// 2. Resize image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(options.getMaxWidth()).setHeight(102);
		MeshBinaryResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "image", params));

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			assertNotNull(contentDao.getLatestDraftFieldContainer(node, english()));
			validateResizeImage(download, contentDao.getLatestDraftFieldContainer(node, english()).getBinary("image"), params, 2048, 102);
		}
	}

	@Test
	public void testTransformImage() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();

		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 100, 118);
	}

	@Test
	public void testTransformWithFocalPoint() throws IOException {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setFocalPoint(0.3f, 0.4f);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		NodeResponse response3 = call(() -> client().findNodeByUuid(projectName(), nodeUuid));

		assertEquals("Focalpoint X did not match.", 0.3f, response3.getFields().getBinaryField("image").getFocalPoint().getX(), 0);
		assertEquals("Focalpoint Y did not match.", 0.4f, response3.getFields().getBinaryField("image").getFocalPoint().getY(), 0);
	}

	@Test
	public void testTransformWithOnlyFocalPoint() throws IOException {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setFocalPoint(0.3f, 0.4f);
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", version, "image", params));
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		NodeResponse response3 = call(() -> client().findNodeByUuid(projectName(), nodeUuid));

		assertEquals("Focalpoint X did not match.", 0.3f, response3.getFields().getBinaryField("image").getFocalPoint().getX(), 0);
		assertEquals("Focalpoint Y did not match.", 0.4f, response3.getFields().getBinaryField("image").getFocalPoint().getY(), 0);
	}

	@Test
	public void testTransformImageCrop() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth(100);
		params.setHeight(200);
		params.setRect(0, 0, 200, 200);

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 100, 200);

	}

	@Test
	public void testTransformImageResizeSmart() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth(200);
		params.setHeight(500);

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 200, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 200, 500);
	}

	@Test
	public void testTransformImageResizeSmartWidthAuto() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth("auto");
		params.setHeight(200);

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 200, transformResponse.getFields().getBinaryField("image").getHeight().intValue());
		assertEquals("The image should use the original width of 1160", 1160, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 1160, 200);
	}

	@Test
	public void testTransformImageResizeSmartHeightAuto() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth(200);
		params.setHeight("auto");

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have benn in the original Height of 1376", 1376, transformResponse.getFields().getBinaryField("image").getHeight().intValue());
		assertEquals("The image should have been resized", 200, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 200, 1376);
	}

	@Test
	public void testTransformImageResizeSmartHeightWidthAuto() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth("auto");
		params.setHeight("auto");

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been in the original width of 1160px", 1160, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		assertEquals("The image should have been in the original height of  1376px", 1376, transformResponse.getFields().getBinaryField("image").getHeight().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 1160, 1376);
	}

	@Test
	public void testTransformImageResizeForce() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		params.setWidth(100);
		params.setHeight(500);
		params.setResizeMode(ResizeMode.FORCE);

		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the resized image
		validateResizeImage(result, null, params, 100, 500);
	}

	@Test
	public void testTransformImageNoParameters() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		// 1. Upload image
		NodeResponse response = uploadImageType(node, "en", "image", extension, fileType);
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// 2. Transform the image
		ImageManipulationParametersImpl params = new ImageManipulationParametersImpl();
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", response.getVersion(), "image", params));
		MeshCoreAssertion.assertThat(testContext).hasUploads(2, 2).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testTransformNonBinary() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// try to transform the "name"
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);

		call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", "draft", "name", params), BAD_REQUEST,
			"error_found_field_is_not_binary", "name");
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testTransformNonImage() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		try (Tx tx = tx()) {
			prepareSchema(node, "*/*", "image");
			tx.success();
		}

		Buffer buffer = Buffer.buffer("I am not an image");
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		NodeResponse response = call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeUuid, "en", "draft", "image",
			new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), "test.txt", "text/plain"));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", response.getVersion(), "image", params), BAD_REQUEST,
			"error_transformation_non_image", "image");
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testResizeWithFocalPoint() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		// 1. Upload image
		NodeResponse response = uploadImageType(node, "en", "image", extension, fileType);

		// 2. Update the binary field and set the focal point via a node update request
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
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "image", params));

		validateResizeImage(result, null, params, 600, 102);
	}

	@Test
	public void testFocalPointZoomWithTooLargeTarget() throws IOException {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		// 1. Upload image
		uploadImageType(node, "en", "image", extension, fileType);

		// 2. Zoom into image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(2048).setHeight(2048);
		params.setFocalPoint(0.5f, 0.5f);
		params.setFocalPointZoom(1.5f);
		params.setCropMode(CropMode.FOCALPOINT);
		call(() -> client().downloadBinaryField(PROJECT_NAME, nodeUuid, "en", "image", params), BAD_REQUEST,
			"image_error_target_too_large_for_zoom");
	}

	@Test
	public void testResizeWithFocalPointOutOfBounds() throws IOException {
		HibNode node = folder("news");

		// 1. Upload image
		NodeResponse response = uploadImageType(node, "en", "image", extension, fileType);

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

	@Test
	public void testTransformEmptyField() throws Exception {
		HibNode node = folder("news");
		String nodeUuid = tx(() -> node.getUuid());

		try (Tx tx = tx()) {
			prepareSchema(node, "image/.*", "image");
			tx.success();
		}

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl();
		call(() -> client().transformNodeBinaryField(PROJECT_NAME, nodeUuid, "en", "draft", "image", params), NOT_FOUND,
			"error_binaryfield_not_found_with_name", "image");

	}

	@Test
	public void testTransformImageFilename() throws Exception {
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		// 1. Upload image
		String version = uploadImageType(node, "en", "image", extension, fileType).getVersion();

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "image", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("image").getWidth().intValue());

		// 3. Validate that a new version was created
		String newNumber = transformResponse.getVersion();
		assertNotEquals("The version number should have changed.", version, newNumber);

		// 4. Download the image
		MeshBinaryResponse result = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "image"));

		// 5. Validate the filename
		assertEquals("blume." + extension, result.getFilename());
	}

	@Test
	public void testWebrootAfterTransform() throws IOException {
		NodeResponse imageNode = createBinaryContent().blockingGet();
		NodeResponse image = uploadImage(imageNode, "en", "binary", extension, fileType);
		String version = image.getVersion();
		String uuid = image.getUuid();
		String path = "/blume." + extension;

		// Make sure that the image is found via webroot
		client().webroot(PROJECT_NAME, path).blockingAwait();

		// 2. Transform the image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100);
		NodeResponse transformResponse = call(() -> client().transformNodeBinaryField(PROJECT_NAME, uuid, "en", version, "binary", params));
		assertEquals("The image should have been resized", 100, transformResponse.getFields().getBinaryField("binary").getWidth().intValue());

		// Make sure that it is still found
		client().webroot(PROJECT_NAME, path).blockingAwait();
	}

	private void validateResizeImage(MeshBinaryResponse download, HibBinaryField binaryField, ImageManipulationParameters params,
									 int expectedWidth, int expectedHeight) throws Exception {
		assertThat(download.getContentType())
			.as("Resized image filetype")
			.isEqualTo("image/" + fileType);

		File targetFile = new File("target", UUID.randomUUID() + "_resized." + extension);
		CountDownLatch latch = new CountDownLatch(1);
		byte[] bytes = IOUtils.toByteArray(download.getStream());
		download.close();
		vertx().fileSystem().writeFile(targetFile.getAbsolutePath(), Buffer.buffer(bytes), rh -> {
			assertTrue(rh.succeeded());
			latch.countDown();
		});
		failingLatch(latch);
		assertThat(targetFile).exists();
		BufferedImage img = ImageIO.read(targetFile);
		//ImageTestUtil.displayImage(img);
		assertEquals(expectedWidth, img.getWidth());
		assertEquals(expectedHeight, img.getHeight());

		if (binaryField != null) {
			CacheFileInfo cacheFile = meshDagger().imageManipulator().getCacheFilePath(binaryField.getBinary().getSHA512Sum(), params).blockingGet();
			assertTrue("The cache file could not be found in the cache directory. {" + cacheFile.path + "}", cacheFile.exists);
		}
	}

}
