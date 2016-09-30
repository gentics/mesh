package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.buffer.Buffer;

public class BinaryFieldUploadVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Test
	public void testUploadWithNoPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");
			role().revokePermissions(node, UPDATE_PERM);

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
		}
	}

	@Test
	@Ignore("mimetype whitelist is not yet implemented")
	public void testUploadWithInvalidMimetype() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			String whitelistRegex = "image/.*";
			prepareSchema(node, whitelistRegex, "binary");

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_error_invalid_mimetype", contentType, whitelistRegex);
		}
	}

	@Test
	public void testUploadMultipleToBinaryNode() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			for (int i = 0; i < 20; i++) {
				BinaryGraphField oldValue = container.getBinary("binary");
				String oldFilename = null;
				if (oldValue != null) {
					oldFilename = oldValue.getFileName();
				}
				String newFileName = "somefile" + i + ".dat";

				call(() -> uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, newFileName));
				node.reload();
				container.reload();

				NodeGraphFieldContainer newContainer = container.getNextVersion();
				assertNotNull("No new version was created.", newContainer);
				assertEquals(newContainer.getUuid(), node.getLatestDraftFieldContainer(english()).getUuid());

				NodeResponse response = readNode(PROJECT_NAME, node.getUuid());
				assertEquals("Check version number", newContainer.getVersion().toString(), response.getVersion().getNumber());
				String value = container.getBinary("binary") == null ? null : container.getBinary("binary").getFileName();
				assertEquals("Version {" + container.getVersion() + "} did not contain the old value", oldFilename, value);
				assertNotNull("Version {" + newContainer.getVersion() + "} did not contain the updated field.", newContainer.getBinary("binary"));
				assertEquals("Version {" + newContainer.getVersion() + "} did not contain the updated value.", newFileName,
						newContainer.getBinary("binary").getFileName());
				container = newContainer;
			}
		}
	}

	@Test
	public void testUploadToNonBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			// Add a schema called nonBinary
			Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new StringFieldSchemaImpl().setName("nonBinary").setLabel("No Binary content"));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "nonBinary", binaryLen, contentType, fileName)
					.invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_found_field_is_not_binary", "nonBinary");
		}
	}

	@Test
	public void testUploadToNodeWithoutBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "nonBinary", binaryLen, contentType, fileName)
					.invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_schema_definition_not_found", "nonBinary");
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

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", fieldKey);

			NodeResponse response = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			String originalVersion = response.getVersion().getNumber();

			// 1. Upload the image
			int size = uploadImage(node.getUuid(), "en", fieldKey, fileName, mimeType);

			response = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			assertNotEquals(originalVersion, response.getVersion().getNumber());
			originalVersion = response.getVersion().getNumber();

			// 2. Upload a non-image 
			fileName = "somefile.dat";
			mimeType = "application/octet-stream";
			GenericMessageResponse message = call(() -> uploadRandomData(node.getUuid(), "en", fieldKey, size, "application/octet-stream", "somefile.dat"));
			expectResponseMessage(message, "node_binary_field_updated", fieldKey);

			node.reload();
			response = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			assertNotEquals(originalVersion, response.getVersion().getNumber());
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

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "node_error_uploadlimit_reached", "9 KB", "9 KB");
		}
	}

	@Test
	public void testPathSegmentation() throws IOException {
		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			node.setUuid(UUIDUtil.randomUUID());

			// Add some test data
			prepareSchema(node, "", "binary");
			String contentType = "application/octet-stream";
			String fileName = "somefile.dat";
			int binaryLen = 10000;
			MeshResponse<GenericMessageResponse> future = uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(future);
			assertSuccess(future);

			// Load the uploaded binary field and return the segment path to the field
			node.reload();
			BinaryGraphField binaryField = node.getLatestDraftFieldContainer(english()).getBinary("binary");
			String uuid = "b677504736ed47a1b7504736ed07a14a";
			binaryField.setUuid(uuid);
			String path = binaryField.getSegmentedPath();
			assertEquals("/b677/5047/36ed/47a1/b750/4736/ed07/a14a/", path);
		}
	}

	@Test
	public void testUpload() throws Exception {

		String contentType = "application/blub";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", "binary");

			GenericMessageResponse message = call(() -> uploadRandomData(node.getUuid(), "en", "binary", binaryLen, contentType, fileName));
			expectResponseMessage(message, "node_binary_field_updated", "binary");

			node.reload();

			NodeResponse response = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
			BinaryField binaryField = response.getFields().getBinaryField("binary");

			assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
			assertEquals("The contentType was correctly set in the response.", contentType, binaryField.getMimeType());
			assertEquals("The binary length was not correctly set in the response.", binaryLen, binaryField.getFileSize());
			assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
			assertNull("The data did contain image information.", binaryField.getDominantColor());
			assertNull("The data did contain image information.", binaryField.getWidth());
			assertNull("The data did contain image information.", binaryField.getHeight());

			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary")
					.invoke();
			latchFor(downloadFuture);
			assertSuccess(downloadFuture);
			NodeDownloadResponse downloadResponse = downloadFuture.result();
			assertNotNull(downloadResponse);
			assertNotNull(downloadResponse.getBuffer().getByte(1));
			assertNotNull(downloadResponse.getBuffer().getByte(binaryLen));
			assertEquals(binaryLen, downloadResponse.getBuffer().length());
			assertEquals(contentType, downloadResponse.getContentType());
			assertEquals(fileName, downloadResponse.getFilename());
		}
	}

	@Test
	public void testUploadWithConflict() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10;
		String fileName = "somefile.dat";

		try (NoTx noTrx = db.noTx()) {
			Node folder2014 = folder("2014");
			Node folder2015 = folder("2015");
			prepareSchema(folder2014, "", "binary");

			// make binary field the segment field
			Schema schema = folder2014.getSchemaContainer().getLatestVersion().getSchema();
			schema.setSegmentField("binary");
			folder2014.getSchemaContainer().getLatestVersion().setSchema(schema);

			// upload file to folder 2014
			MeshResponse<GenericMessageResponse> uploadFuture = uploadRandomData(folder2014.getUuid(), "en", "binary", binaryLen, contentType,
					fileName).invoke();
			latchFor(uploadFuture);
			assertSuccess(uploadFuture);

			// try to upload same file to folder 2015
			uploadFuture = uploadRandomData(folder2015.getUuid(), "en", "binary", binaryLen, contentType, fileName).invoke();
			latchFor(uploadFuture);
			expectException(uploadFuture, CONFLICT, "node_conflicting_segmentfield_upload", "binary", fileName);
		}
	}

	@Test
	public void testUploadImage() throws IOException {
		String contentType = "image/png";
		String fieldName = "image";
		String fileName = "somefile.png";

		try (NoTx noTrx = db.noTx()) {
			Node node = folder("news");
			prepareSchema(node, "", fieldName);

			int size = uploadImage(node.getUuid(), "en", fieldName, fileName, contentType);

			node.reload();
			NodeResponse response = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));

			BinaryField binaryField = response.getFields().getBinaryField(fieldName);
			assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
			assertEquals("The contentType was correctly set in the response.", contentType, binaryField.getMimeType());
			assertEquals("The binary length was not correctly set in the response.", size, binaryField.getFileSize());
			assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
			assertEquals("The data did not contain correct image color information.", "#737042", binaryField.getDominantColor());
			assertEquals("The data did not contain correct image width information.", 1160, binaryField.getWidth().intValue());
			assertEquals("The data did not contain correct image height information.", 1376, binaryField.getHeight().intValue());

			MeshResponse<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", fieldName)
					.invoke();
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

	private int uploadImage(String uuid, String languageTag, String fieldname, String filename, String contentType) throws IOException {
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);
		GenericMessageResponse message = call(
				() -> getClient().updateNodeBinaryField(PROJECT_NAME, uuid, languageTag, fieldname, buffer, filename, contentType));

		expectResponseMessage(message, "node_binary_field_updated", fieldname);
		return bytes.length;

	}

}