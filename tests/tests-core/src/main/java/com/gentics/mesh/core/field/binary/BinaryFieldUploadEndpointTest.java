package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
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
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.cmdline.params.LanguageParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.assertj.MeshCoreAssertion;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BinaryFieldUploadEndpointTest extends AbstractMeshTest {

	@Test
	public void testUploadWithNoPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			prepareSchema(node, "", "binary");
			roleDao.revokePermissions(role(), node, UPDATE_PERM);
			tx.success();
		}

		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), FORBIDDEN, "error_missing_perm", uuid,
			UPDATE_PERM.getRestPerm().getName());

	}

	@Test
	@Ignore("mimetype whitelist is not yet implemented")
	public void testUploadWithInvalidMimetype() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		String whitelistRegex = "image/.*";
		HibNode node = folder("news");

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
		HibNode node = folder("news");

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
		disableAutoPurge();

		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		HibNode node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		for (int i = 0; i < 20; i++) {
			String newFileName = "somefile" + i + ".dat";
			String oldFilename = null;
			HibNodeFieldContainer container = tx(() -> boot().contentDao().getFieldContainer(node, "en"));
			try (Tx tx = tx()) {
				HibBinaryField oldValue = container.getBinary("binary");
				if (oldValue != null) {
					oldFilename = oldValue.getFileName();
				}
			}

			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, newFileName));

			try (Tx tx = tx()) {
				ContentDao contentDao = tx.contentDao();
				HibNodeFieldContainer newContainer = contentDao.getNextVersions(container).iterator().next();
				assertNotNull("No new version was created.", newContainer);
				assertEquals(newContainer.getUuid(), contentDao.getLatestDraftFieldContainer(node, english()).getUuid());

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
			HibNode node = folder("news");

			// Add a schema called nonBinary
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
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
			HibNode node = folder("news");

			// Add a schema called nonBinary
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			for (String fieldName : fields) {
				schema.addField(FieldUtil.createBinaryFieldSchema(fieldName));
			}
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
		}

		String uuid = tx(() -> folder("news").getUuid());
		VersionNumber version = tx(() -> boot().contentDao().getFieldContainer(folder("news"), "en").getVersion());

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
		}).lastOrError().ignoreElement().blockingAwait();

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		for (String field : fields) {
			BinaryField binaryField = response.getFields().getBinaryField(field);
			assertNotNull(binaryField.getDominantColor());
			assertNotNull(binaryField.getWidth());
			assertNotNull(binaryField.getHeight());
			assertEquals("image/jpeg", binaryField.getMimeType());
		}
	}

	/**
	 * Test parallel upload of the same binary data - thus the same binary vertex should be used.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testParallelDupUpload() throws IOException {

		String folderUuid = tx(() -> folder("news").getUuid());
		// Prepare schema
		try (Tx tx = tx()) {
			HibNode node = folder("news");
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(FieldUtil.createBinaryFieldSchema("image"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
		}

		Buffer buffer = getBuffer("/pictures/blume.jpg");
		Observable.range(0, 200).flatMapSingle(number -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(folderUuid);
			request.setSchemaName("folder");
			request.getFields().put("slug", FieldUtil.createStringField("folder" + number));
			return client().createNode(PROJECT_NAME, request).toSingle()
				.flatMap(node -> {
					byte[] data = buffer.getBytes();
					int size = data.length;
					InputStream ins = new ByteArrayInputStream(data);
					return client()
						.updateNodeBinaryField(projectName(), node.getUuid(), "en", node.getVersion(), "image", ins, size, "blume.jpg", "image/jpeg")
						.toSingle();
				});
		}).lastOrError().ignoreElement().blockingAwait();

	}

	@Test
	public void testUploadBrokenImage() throws IOException {
		String contentType = "image/jpeg";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			HibNode node = folder("news");

			// Add a schema called nonBinary
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
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
		HibNode node = folder("news");

		// Add a schema called nonBinary
		try (Tx tx = tx()) {
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(FieldUtil.createBinaryFieldSchema("image"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			tx.success();
		}

		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		for (int i = 0; i < 100; i++) {
			String fileName = "somefile" + i + ".dat";
			call(() -> uploadRandomData(node, "en", "image", binaryLen, contentType, fileName));
		}
		MeshCoreAssertion.assertThat(testContext).hasUploadFiles(100).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testUploadExif() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
		Buffer buffer = getBuffer("/pictures/android-gps.jpg");
		NodeResponse node = createBinaryNode(parentNodeUuid);
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), "test.jpg", "image/jpeg"));

		NodeResponse node2 = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()));
		System.out.println(node2.toJson());
		BinaryField binaryField = node2.getFields().getBinaryField("binary");
		BinaryMetadata metadata2 = binaryField.getMetadata();
		assertEquals(13.920556, metadata2.getLocation().getLon().doubleValue(), 0.01);
		assertEquals(47.6725, metadata2.getLocation().getLat().doubleValue(), 0.01);
		assertEquals(1727, metadata2.getLocation().getAlt().intValue());
		assertEquals("4.2", metadata2.get("exif_FocalLength"));
		assertNull("The jpeg should not have any extracted content.", binaryField.getPlainText());

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
			NodeResponse node = createBinaryNode(parentNodeUuid);
			NodeResponse node2 = call(
				() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
					buffer.length(), file, "application/pdf"));
			assertFalse("Metadata could not be found for file {" + file + "}",
				node2.getFields().getBinaryField("binary").getMetadata().getMap().isEmpty());
		}

	}

	@Test
	public void testPlainTextExtractionForDocuments() throws IOException {
		expectPlainText("test.pdf", "application/pdf", "Enemenemu");
		expectPlainText("test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"Das ist ein Word Dokument für den Johannes");
		expectPlainText("small.mp4", "application/pdf", "HandBrake 0.9.4 2009112300");
	}

	private void expectPlainText(String fileName, String mimeType, String plainText) throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		Buffer buffer = getBuffer("/testfiles/" + fileName);
		NodeResponse node = createBinaryNode(parentNodeUuid);
		NodeResponse node2 = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
				buffer.length(), fileName, mimeType));
		BinaryField binaryField = node2.getFields().getBinaryField("binary");
		assertEquals("The plain text of file {" + fileName + "} did not match", plainText, binaryField.getPlainText());
	}

	@Test
	public void testUploadToNodeWithoutBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		HibNode node = folder("news");

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
		HibNode node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", fieldKey);
			tx.success();
		}

		String nodeUuid = tx(() -> node.getUuid());
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft()));
		String originalVersion = response.getVersion();

		// 1. Upload the image
		int size = uploadImage(node, "en", fieldKey, fileName, mimeType);

		response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft()));
		assertNotEquals(originalVersion, response.getVersion());
		originalVersion = response.getVersion();

		// 2. Upload a non-image
		fileName = "somefile.dat";
		mimeType = "application/octet-stream";
		response = call(() -> uploadRandomData(node, "en", fieldKey, size, "application/octet-stream", "somefile.dat"));
		assertNotNull(response);

		response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft()));
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

	@Test
	public void testFileUploadLimit() throws IOException {

		int binaryLen = 10000;
		options().getUploadOptions().setByteLimit(binaryLen - 1);
		String contentType = "application/octet-stream";
		String fileName = "somefile.dat";
		HibNode node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), BAD_REQUEST, "node_error_uploadlimit_reached",
			"9 KB", "9 KB");

		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testSingleUpload() throws IOException {
		int binaryLen = 10000;
		String contentType = "application/octet-stream";
		String fileName = "somefile.dat";
		HibNode node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}

		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testUpload() throws Exception {

		String contentType = "application/blub";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());
		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		NodeResponse response = call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

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
			ContentDao contentDao = tx.contentDao();
			HibBinaryField binaryGraphField = contentDao.getLatestDraftFieldContainer(node, english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			String path = localBinaryStorage().getFilePath(binaryUuid);
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
		HibNode node = folder("news");
		String uuid = tx(() -> node.getUuid());
		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		File binaryFile;
		String hash;
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibBinaryField binaryGraphField = contentDao.getLatestDraftFieldContainer(node, english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFile = new File(localBinaryStorage().getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFile.exists());
			hash = binaryGraphField.getBinary().getSHA512Sum();
		}

		call(() -> client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(true)));

		HibBinary binary = tx(tx -> {
			return HibClassConverter.toGraph(tx).binaries().findByHash(hash).runInExistingTx(tx);
		});

		assertNull("The binary for the hash should have also been removed since only one node used the binary.", binary);
		assertFalse("The binary file should have been removed.", binaryFile.exists());
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 1).hasTempFiles(0).hasTempUploads(0);

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
		HibNode node = folder("news");
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
	public void testDeleteBinaryNodeDeduplication() throws IOException {
		// The data
		String contentType = "application/blub";
		int binaryLen = 8000;
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		String fileNameA = "somefile-a.dat";
		String fileNameB = "somefile-b.dat";

		// The test nodes
		HibNode nodeA = folder("news");
		String uuidA = tx(() -> nodeA.getUuid());
		String versionA = tx(() -> boot().contentDao().getFieldContainer(nodeA, "en").getVersion()).toString();

		HibNode nodeB = folder("products");
		String uuidB = tx(() -> nodeB.getUuid());
		String versionB = tx(() -> boot().contentDao().getFieldContainer(nodeA, "en").getVersion()).toString();

		// Setup the schemas
		try (Tx tx = tx()) {
			prepareSchema(nodeA, "", "binary");
			prepareSchema(nodeB, "", "binary");
			tx.success();
		}
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);

		// Upload the binary in both nodes
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidA, "en", versionA, "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), fileNameA, contentType));
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidB, "en", versionB, "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), fileNameB, contentType));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		File binaryFileA;
		String hashA;
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibBinaryField binaryGraphField = contentDao.getLatestDraftFieldContainer(nodeA, english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFileA = new File(localBinaryStorage().getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFileA.exists());
			hashA = binaryGraphField.getBinary().getSHA512Sum();
		}

		File binaryFileB;
		String hashB;
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibBinaryField binaryGraphField = contentDao.getLatestDraftFieldContainer(nodeB, english()).getBinary("binary");
			String binaryUuid = binaryGraphField.getBinary().getUuid();
			binaryFileB = new File(localBinaryStorage().getFilePath(binaryUuid));
			assertTrue("The binary file could not be found.", binaryFileB.exists());
			hashB = binaryGraphField.getBinary().getSHA512Sum();
		}
		assertEquals(hashA, hashB);
		assertEquals(binaryFileA.getAbsolutePath(), binaryFileB.getAbsolutePath());

		// Now delete nodeA
		call(() -> client().deleteNode(PROJECT_NAME, uuidA, new DeleteParametersImpl().setRecursive(true)));

		HibBinary binaryA = tx(tx -> {
			return HibClassConverter.toGraph(tx).binaries().findByHash(hashA).runInExistingTx(tx);
		});

		assertNotNull("The binary for the hash should not have been removed since it is still in use.", binaryA);
		assertTrue("The binary file should not have been deleted since there is still one node which uses it.", binaryFileA.exists());
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		// Now delete nodeB
		call(() -> client().deleteNode(PROJECT_NAME, uuidB, new DeleteParametersImpl().setRecursive(true)));

		binaryA = tx(tx -> {
			return HibClassConverter.toGraph(tx).binaries().findByHash(hashA).runInExistingTx(tx);
		});
		assertNull("The binary for the hash should have also been removed since only one node used the binary.", binaryA);
		assertFalse("The binary file should have been removed.", binaryFileA.exists());

		// The folder is not removed. Removing the parent folder of the upload would require us to lock uploads.
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 1).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testUploadWithSegmentfieldConflict() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10;
		String fileName = "somefile.dat";

		// 1. Prepare the folder schema
		try (Tx tx = tx()) {
			HibNode folder2014 = folder("2014");
			prepareSchema(folder2014, "", "binary");

			// make binary field the segment field
			SchemaVersionModel schema = folder2014.getSchemaContainer().getLatestVersion().getSchema();
			schema.setSegmentField("binary");
			folder2014.getSchemaContainer().getLatestVersion().setSchema(schema);
			tx.success();
		}

		// 2. Update node a
		MeshCoreAssertion.assertThat(testContext).hasUploads(0, 0).hasTempFiles(0).hasTempUploads(0);
		HibNode folder2014 = folder("2014");
		// upload file to folder 2014
		call(() -> uploadRandomData(folder2014, "en", "binary", binaryLen, contentType, fileName));
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);

		call(() -> client().findNodeByUuid(PROJECT_NAME, db().tx(() -> folder("2014").getUuid()), new NodeParametersImpl().setResolveLinks(
			LinkType.FULL)));

		HibNode folder2015 = folder("2015");

		// try to upload same file to folder 2015
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);
		call(() -> uploadRandomData(folder2015, "en", "binary", binaryLen, contentType, fileName), CONFLICT,
			"node_conflicting_segmentfield_upload", "binary", fileName);
		MeshCoreAssertion.assertThat(testContext).hasUploads(1, 1).hasTempFiles(0).hasTempUploads(0);
	}

	@Test
	public void testUploadImage() throws IOException {
		String contentType = "image/png";
		String fieldName = "image";
		String fileName = "somefile.png";
		HibNode node = folder("news");

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
		HibNode node = folder("news");

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

}
