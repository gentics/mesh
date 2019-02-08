package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.rest.client.MeshBinaryResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class BinaryFieldUploadEndpointTest extends AbstractMeshTest {

	@Test
	public void testUploadWithNoPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			role().revokePermissions(node, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), FORBIDDEN, "error_missing_perm", node.getUuid(),
				UPDATE_PERM.getRestPerm().getName());
		}

	}

	@Test
	@Ignore("mimetype whitelist is not yet implemented")
	public void testUploadWithInvalidMimetype() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		String whitelistRegex = "image/.*";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, whitelistRegex, "binary");
			tx.success();
		}
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), BAD_REQUEST, "node_error_invalid_mimetype", contentType,
			whitelistRegex);

	}

	@Test
	public void testUploadBogusName() throws IOException {
		String contentType = "application/octet-stream";

		int binaryLen = 10000;
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, "somefile.DAT"));
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, "application/pdf", "somefile.PDF"));
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, "application/pdf", "somefile."));
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, "application/pdf", "somefile"));
	}

	@Test
	public void testUploadMultipleToBinaryNode() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		for (int i = 0; i < 20; i++) {
			String newFileName = "somefile" + i + ".dat";
			String oldFilename = null;
			NodeGraphFieldContainer container = tx(() -> node.getGraphFieldContainer("en"));
			try (Tx tx = tx()) {
				BinaryGraphField oldValue = container.getBinary("binary");
				if (oldValue != null) {
					oldFilename = oldValue.getFileName();
				}
			}

			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, newFileName));

			try (Tx tx = tx()) {
				NodeGraphFieldContainer newContainer = container.getNextVersions().iterator().next();
				assertNotNull("No new version was created.", newContainer);
				assertEquals(newContainer.getUuid(), node.getLatestDraftFieldContainer(english()).getUuid());

				NodeResponse response = readNode(PROJECT_NAME, node.getUuid());
				assertEquals("Check version number", newContainer.getVersion().toString(), response.getVersion());
				String value = container.getBinary("binary") == null ? null : container.getBinary("binary").getFileName();
				assertEquals("Version {" + container.getVersion() + "} did not contain the old value", oldFilename, value);
				assertNotNull("Version {" + newContainer.getVersion() + "} did not contain the updated field.", newContainer.getBinary("binary"));
				assertEquals("Version {" + newContainer.getVersion() + "} did not contain the updated value.", newFileName, newContainer.getBinary(
					"binary").getFileName());
				container = newContainer;
			}
		}

	}

	@Test
	public void testUploadToNonBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			Node node = folder("news");

			// Add a schema called nonBinary
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new StringFieldSchemaImpl().setName("nonBinary").setLabel("No Binary content"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			call(() -> uploadRandomData(node, "en", "nonBinary", binaryLen, contentType, fileName), BAD_REQUEST, "error_found_field_is_not_binary",
				"nonBinary");
		}
	}

	@Test
	public void testParallelImageUpload() throws IOException {
		String fileName = "blume.jpg";
		String contentType = "image/jpeg";

		List<String> fields = Arrays.asList("blume", "blume2", "dreamtime");

		try (Tx tx = tx()) {
			Node node = folder("news");

			// Add a schema called nonBinary
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			for (String fieldName : fields) {
				schema.addField(FieldUtil.createBinaryFieldSchema(fieldName));
			}
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
		}

		String uuid = tx(() -> folder("news").getUuid());
		VersionNumber version = tx(() -> folder("news").getGraphFieldContainer("en").getVersion());

		Map<String, Buffer> data = new HashMap<>();
		for (String field : fields) {
			Buffer buffer = getBuffer("/pictures/" + field + ".jpg");
			data.put(field, buffer);
		}

		Observable.fromIterable(fields).flatMapSingle(fieldName -> {
			Buffer buffer = data.get(fieldName);
			return client()
				.updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), fieldName, new ByteArrayInputStream(buffer.getBytes()),
					buffer.length(), fileName, contentType)
				.toSingle();
		}).lastOrError().toCompletable().blockingAwait();

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		for (String field : fields) {
			BinaryField binaryField = response.getFields().getBinaryField(field);
			assertNotNull(binaryField.getDominantColor());
			assertNotNull(binaryField.getWidth());
			assertNotNull(binaryField.getHeight());
			assertEquals("image/jpeg", binaryField.getMimeType());
		}
	}

	@Test
	public void testUploadBrokenImage() throws IOException {
		String contentType = "image/jpeg";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			Node node = folder("news");

			// Add a schema called nonBinary
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(FieldUtil.createBinaryFieldSchema("image"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			call(() -> uploadRandomData(node, "en", "image", binaryLen, contentType, fileName));
		}

		String uuid = tx(() -> folder("news").getUuid());
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		BinaryField binaryField = response.getFields().getBinaryField("image");
		assertNull(binaryField.getDominantColor());
		assertNull(binaryField.getWidth());
		assertNull(binaryField.getHeight());
		assertEquals("image/jpeg", binaryField.getMimeType());
	}

	@Test
	public void testUploadMultipleBrokenImages() {
		String contentType = "image/jpeg";
		int binaryLen = 10000;

		try (Tx tx = tx()) {
			Node node = folder("news");

			// Add a schema called nonBinary
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(FieldUtil.createBinaryFieldSchema("image"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			for (int i = 0; i < 100; i++) {
				String fileName = "somefile" + i + ".dat";
				call(() -> uploadRandomData(node, "en", "image", binaryLen, contentType, fileName));
			}
		}
	}

	@Test
	public void testUploadExif() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
		Buffer buffer = getBuffer("/pictures/android-gps.jpg");
		NodeResponse node = createNode(parentNodeUuid);
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), "test.jpg", "image/jpeg"));

		NodeResponse node2 = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()));
		BinaryMetadata metadata2 = node2.getFields().getBinaryField("binary").getMetadata();
		assertEquals(13.920556, metadata2.getLocation().getLon().doubleValue(), 0.01);
		assertEquals(47.6725, metadata2.getLocation().getLat().doubleValue(), 0.01);
		assertEquals(1727, metadata2.getLocation().getAlt().intValue());
		assertEquals("4.2 mm", metadata2.get("Focal_Length"));

		NodeUpdateRequest nodeUpdateRequest = node2.toRequest();
		BinaryField field = nodeUpdateRequest.getFields().getBinaryField("binary");
		field.getMetadata().clear();
		field.getMetadata().add("dummy", "value");
		nodeUpdateRequest.getFields().put("binary", field);
		NodeResponse node3 = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest));

		BinaryMetadata metadata3 = node3.getFields().getBinaryField("binary").getMetadata();
		assertEquals("value", metadata3.get("dummy"));

		// Upload the image again and check that the metadata will be updated
		NodeResponse node4 = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", node3.getVersion(), "binary",
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));
		BinaryMetadata metadata4 = node4.getFields().getBinaryField("binary").getMetadata();
		assertEquals(13.920556, metadata4.getLocation().getLon().doubleValue(), 0.01);

	}

	@Test
	public void testUploadFilesForTika() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		List<String> files = Arrays.asList("small.mp4", "small.ogv", "test.pdf", "test.docx");
		for (String file : files) {
			Buffer buffer = getBuffer("/testfiles/" + file);
			NodeResponse node = createNode(parentNodeUuid);
			NodeResponse node2 = call(
				() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
					buffer.length(), file, "application/pdf"));
			assertFalse("Metadata could not be found for file {" + file + "}",
				node2.getFields().getBinaryField("binary").getMetadata().getMap().isEmpty());
		}

	}

	@Test
	public void testUploadToNodeWithoutBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		Node node = folder("news");

		try (Tx tx = tx()) {
			call(() -> uploadRandomData(node, "en", "nonBinary", binaryLen, contentType, fileName), BAD_REQUEST, "error_schema_definition_not_found",
				"nonBinary");
		}
	}

	/**
	 * Test whether the implementation works as expected when you update the node binary data to an image and back to a non image. The image related fields
	 * should disappear.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdateBinaryToImageAndNonImage() throws IOException {
		String mimeType = "image/png";
		String fieldKey = "image";
		String fileName = "somefile.png";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", fieldKey);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			String originalVersion = response.getVersion();

			// 1. Upload the image
			int size = uploadImage(node, "en", fieldKey, fileName, mimeType);

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertNotEquals(originalVersion, response.getVersion());
			originalVersion = response.getVersion();

			// 2. Upload a non-image
			fileName = "somefile.dat";
			mimeType = "application/octet-stream";
			response = call(() -> uploadRandomData(node, "en", fieldKey, size, "application/octet-stream", "somefile.dat"));
			assertNotNull(response);

			response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertNotEquals(originalVersion, response.getVersion());
			BinaryField binaryField = response.getFields().getBinaryField(fieldKey);

			assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
			assertEquals("The contentType was correctly set in the response.", mimeType, binaryField.getMimeType());
			assertEquals("The binary length was not correctly set in the response.", size, binaryField.getFileSize());
			assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
			assertNull("The data did contain image information.", binaryField.getWidth());
			assertNull("The data did contain image information.", binaryField.getHeight());
			assertNull("The data did contain image information.", binaryField.getDominantColor());
		}

	}

	@Test
	public void testFileUploadLimit() throws IOException {

		int binaryLen = 10000;
		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(binaryLen - 1);
		String contentType = "application/octet-stream";
		String fileName = "somefile.dat";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), BAD_REQUEST, "node_error_uploadlimit_reached",
				"9 KB", "9 KB");
		}
	}

	@Test
	public void testUpload() throws Exception {

		String contentType = "application/blub";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		String uuid = tx(() -> node.getUuid());
		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		File uploadFolder = new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory());
		FileUtils.deleteDirectory(uploadFolder);

		NodeResponse response = call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		assertTrue("The upload should have created the tmp folder", uploadFolder.exists());
		Thread.sleep(1000);
		assertThat(uploadFolder.list()).as("Folder should not contain any remaining tmp upload file").isEmpty();

		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
		BinaryField binaryField = response.getFields().getBinaryField("binary");

		assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
		assertEquals("The contentType was correctly set in the response.", contentType, binaryField.getMimeType());
		assertEquals("The binary length was not correctly set in the response.", binaryLen, binaryField.getFileSize());
		assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
		assertNull("The data did contain image information.", binaryField.getDominantColor());
		assertNull("The data did contain image information.", binaryField.getWidth());
		assertNull("The data did contain image information.", binaryField.getHeight());

		MeshBinaryResponse downloadResponse = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "binary"));
		assertNotNull(downloadResponse);
		byte[] bytes = IOUtils.toByteArray(downloadResponse.getStream());
		downloadResponse.close();
		assertNotNull(bytes[0]);
		assertNotNull(bytes[binaryLen - 1]);
		assertEquals(binaryLen, bytes.length);
		assertEquals(contentType, downloadResponse.getContentType());
		assertEquals(fileName, downloadResponse.getFilename());

		try (Tx tx = tx()) {
			BinaryGraphField binaryGraphField = node.getLatestDraftFieldContainer(english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			String path = LocalBinaryStorage.getFilePath(binaryUuid);
			File binaryFile = new File(path);
			assertTrue("The binary file could not be found.", binaryFile.exists());
			assertEquals("The expected length of the file did not match.", binaryLen, binaryFile.length());
		}

	}

	/**
	 * Assert that deleting a binary node will also remove the stored binary file.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteBinaryNode() throws IOException {

		String contentType = "application/blub";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		String uuid = tx(() -> node.getUuid());
		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		File uploadFolder = new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory());
		FileUtils.deleteDirectory(uploadFolder);

		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

		File binaryFile;
		String hash;
		try (Tx tx = tx()) {
			BinaryGraphField binaryGraphField = node.getLatestDraftFieldContainer(english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFile = new File(LocalBinaryStorage.getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFile.exists());
			hash = binaryGraphField.getBinary().getSHA512Sum();
		}

		call(() -> client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(true)));
		try (Tx tx = tx()) {
			assertNull("The binary for the hash should have also been removed since only one node used the binary.", meshRoot().getBinaryRoot()
				.findByHash(hash));
		}
		assertFalse("The binary file should have been removed.", binaryFile.exists());

	}

	/**
	 * Assert that a binary cannot be uploaded if the filename or content type is empty.
	 *
	 * @throws IOException
	 */
	@Test
	public void testUploadBinaryWithEmptyProperties() throws IOException {
		String binaryFieldName = "binary";
		// The test nodes
		Node node = folder("news");
		// Setup the schemas
		try (Tx tx = tx()) {
			prepareSchema(node, "", binaryFieldName);
			tx.success();
		}
		// 1. Upload some binary data without filename
		call(() -> uploadRandomData(node, "en", binaryFieldName, 8000, "application/octet-stream", ""), BAD_REQUEST,
			"field_binary_error_emptyfilename", binaryFieldName);

		// 2. Upload some binary data without content type
		try {
			uploadRandomData(node, "en", binaryFieldName, 8000, "", "filename.dat").blockingAwait();
			fail("Uploading data without contentype should cause an exception");
		} catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalArgumentException.class);
		}
	}

	/**
	 * Assert that deleting one node will not affect the binary of another node which uses the same binary (binary of the binaryfield).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteBinaryNodeDeuplication() throws IOException {
		// The data
		String contentType = "application/blub";
		int binaryLen = 8000;
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		String fileName = "somefile.dat";

		// The test nodes
		Node nodeA = folder("news");
		String uuidA = tx(() -> nodeA.getUuid());
		String versionA = tx(() -> nodeA.getGraphFieldContainer("en").getVersion()).toString();

		Node nodeB = folder("products");
		String uuidB = tx(() -> nodeB.getUuid());
		String versionB = tx(() -> nodeA.getGraphFieldContainer("en").getVersion()).toString();

		// Setup the schemas
		try (Tx tx = tx()) {
			prepareSchema(nodeA, "", "binary");
			prepareSchema(nodeB, "", "binary");
			tx.success();
		}
		// Clear the upload folder
		File uploadFolder = new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory());
		FileUtils.deleteDirectory(uploadFolder);

		// Upload the binary in both nodes
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidA, "en", versionA, "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), fileName, contentType));
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidB, "en", versionB, "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), fileName, contentType));

		File binaryFileA;
		String hashA;
		try (Tx tx = tx()) {
			BinaryGraphField binaryGraphField = nodeA.getLatestDraftFieldContainer(english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFileA = new File(LocalBinaryStorage.getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFileA.exists());
			hashA = binaryGraphField.getBinary().getSHA512Sum();
		}

		File binaryFileB;
		String hashB;
		try (Tx tx = tx()) {
			BinaryGraphField binaryGraphField = nodeB.getLatestDraftFieldContainer(english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFileB = new File(LocalBinaryStorage.getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFileB.exists());
			hashB = binaryGraphField.getBinary().getSHA512Sum();
		}
		assertEquals(hashA, hashB);
		assertEquals(binaryFileA.getAbsolutePath(), binaryFileB.getAbsolutePath());

		// Now delete nodeA
		call(() -> client().deleteNode(PROJECT_NAME, uuidA, new DeleteParametersImpl().setRecursive(true)));
		try (Tx tx = tx()) {
			assertNotNull("The binary for the hash should not have been removed since it is still in use.", meshRoot().getBinaryRoot().findByHash(
				hashA));
		}
		assertTrue("The binary file should not have been deleted since there is still one node which uses it.", binaryFileA.exists());

		// Now delete nodeB
		call(() -> client().deleteNode(PROJECT_NAME, uuidB, new DeleteParametersImpl().setRecursive(true)));

		try (Tx tx = tx()) {
			assertNull("The binary for the hash should have also been removed since only one node used the binary.", meshRoot().getBinaryRoot()
				.findByHash(hashA));
		}
		assertFalse("The binary file should have been removed.", binaryFileA.exists());

	}

	@Test
	public void testUploadWithSegmentfieldConflict() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10;
		String fileName = "somefile.dat";

		// 1. Prepare the folder schema
		try (Tx tx = tx()) {
			Node folder2014 = folder("2014");
			prepareSchema(folder2014, "", "binary");

			// make binary field the segment field
			SchemaModel schema = folder2014.getSchemaContainer().getLatestVersion().getSchema();
			schema.setSegmentField("binary");
			folder2014.getSchemaContainer().getLatestVersion().setSchema(schema);
			tx.success();
		}

		// 2. Update node a
		try (Tx tx = tx()) {
			// upload file to folder 2014
			Node folder2014 = folder("2014");
			call(() -> uploadRandomData(folder2014, "en", "binary", binaryLen, contentType, fileName));
		}

		call(() -> client().findNodeByUuid(PROJECT_NAME, db().tx(() -> folder("2014").getUuid()), new NodeParametersImpl().setResolveLinks(
			LinkType.FULL)));

		try (Tx tx = tx()) {
			// try to upload same file to folder 2015
			Node folder2015 = folder("2015");
			call(() -> uploadRandomData(folder2015, "en", "binary", binaryLen, contentType, fileName), CONFLICT,
				"node_conflicting_segmentfield_upload", "binary", fileName);
		}

	}

	@Test
	public void testUploadImage() throws IOException {
		String contentType = "image/png";
		String fieldName = "image";
		String fileName = "somefile.png";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", fieldName);
			tx.success();
		}

		try (Tx tx = tx()) {
			int size = uploadImage(node, "en", fieldName, fileName, contentType);
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));

			BinaryField binaryField = response.getFields().getBinaryField(fieldName);
			assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
			assertEquals("The contentType was correctly set in the response.", contentType, binaryField.getMimeType());
			assertEquals("The binary length was not correctly set in the response.", size, binaryField.getFileSize());
			assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
			assertEquals("The data did not contain correct image color information.", "#737042", binaryField.getDominantColor());
			assertEquals("The data did not contain correct image width information.", 1160, binaryField.getWidth().intValue());
			assertEquals("The data did not contain correct image height information.", 1376, binaryField.getHeight().intValue());

			MeshBinaryResponse downloadResponse = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", fieldName));
			assertNotNull(downloadResponse);
			byte[] bytes = IOUtils.toByteArray(downloadResponse.getStream());
			downloadResponse.close();
			assertEquals(size, bytes.length);
			assertNotNull("The first byte of the response could not be loaded.", bytes[0]);
			assertNotNull("The last byte of the response could not be loaded.", bytes[size - 1]);
			assertEquals(contentType, downloadResponse.getContentType());
			assertEquals(fileName, downloadResponse.getFilename());
		}
	}

	@Test
	public void testFlowableDownload() throws IOException {
		String contentType = "image/png";
		String fieldName = "image";
		String fileName = "somefile.png";
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", fieldName);
			tx.success();
		}

		try (Tx tx = tx()) {
			int size = uploadImage(node, "en", fieldName, fileName, contentType);

			MeshBinaryResponse downloadResponse = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", fieldName));
			assertNotNull(downloadResponse);
			byte[] bytes = downloadResponse.getFlowable().reduce(ArrayUtils::addAll).blockingGet();
			assertEquals(size, bytes.length);
			assertNotNull("The first byte of the response could not be loaded.", bytes[0]);
			assertNotNull("The last byte of the response could not be loaded.", bytes[size - 1]);
			assertEquals(contentType, downloadResponse.getContentType());
			assertEquals(fileName, downloadResponse.getFilename());
		}
	}

	private NodeResponse createNode(String parentNodeUuid) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setSchemaName("binary_content");
		return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
	}

}