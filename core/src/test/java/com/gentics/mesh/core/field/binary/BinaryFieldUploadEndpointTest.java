package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

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
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName), FORBIDDEN, "error_missing_perm", node.getUuid());
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
				NodeGraphFieldContainer newContainer = container.getNextVersion();
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
			assertNull("The data did contain image information.", binaryField.getDominantColor());
			assertNull("The data did contain image information.", binaryField.getWidth());
			assertNull("The data did contain image information.", binaryField.getHeight());
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

		NodeDownloadResponse downloadResponse = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", "binary"));
		assertNotNull(downloadResponse);
		assertNotNull(downloadResponse.getBuffer().getByte(1));
		assertNotNull(downloadResponse.getBuffer().getByte(binaryLen));
		assertEquals(binaryLen, downloadResponse.getBuffer().length());
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
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidA, "en", versionA, "binary", buffer, fileName, contentType));
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuidB, "en", versionB, "binary", buffer, fileName, contentType));

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

			MeshResponse<NodeDownloadResponse> downloadFuture = client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", fieldName).invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);
			NodeDownloadResponse downloadResponse = downloadFuture.result();
			assertNotNull(downloadResponse);
			assertNotNull("The first byte of the response could not be loaded.", downloadResponse.getBuffer().getByte(1));
			assertNotNull("The last byte of the response could not be loaded.", downloadResponse.getBuffer().getByte(size));
			assertEquals(size, downloadResponse.getBuffer().length());
			assertEquals(contentType, downloadResponse.getContentType());
			assertEquals(fileName, downloadResponse.getFilename());
		}
	}

	private int uploadImage(Node node, String languageTag, String fieldname, String filename, String contentType) throws IOException {
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);
		String uuid = node.getUuid();
		VersionNumber version = node.getGraphFieldContainer(languageTag).getVersion();
		NodeResponse response = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, languageTag, version.toString(), fieldname, buffer,
				filename, contentType));
		assertNotNull(response);
		return bytes.length;

	}

}