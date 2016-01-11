package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.node.AbstractBinaryVerticleTest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Future;

public class BinaryGraphNodeVerticleTest extends AbstractBinaryVerticleTest {

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testUploadWithNoPerm() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		prepareSchema(node, "", "binary");
		role().revokePermissions(node, UPDATE_PERM);

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
	}

	@Test
	public void testUploadWithInvalidMimetype() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		String whitelistRegex = "image/.*";
		prepareSchema(node, whitelistRegex, "binary");

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_error_invalid_mimetype", contentType, whitelistRegex);
	}

	@Test
	public void testUploadTwiceToBinaryNode() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		Node node = folder("news");
		prepareSchema(node, "", "binary");

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

		future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

	}

	@Test
	public void testUploadToNonBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		Node node = folder("news");

		// Add a schema called nonBinary
		Schema schema = node.getSchemaContainer().getSchema();
		schema.addField(new StringFieldSchemaImpl().setName("nonBinary").setLabel("No Binary content"));
		node.getSchemaContainer().setSchema(schema);
		getClient().getClientSchemaStorage().addSchema(schema);

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "nonBinary", binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_found_field_is_not_binary", "nonBinary");
	}

	@Test
	public void testUploadToNodeWithoutBinaryField() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		Node node = folder("news");

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "nonBinary", binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_schema_definition_not_found", "nonBinary");
	}

	/**
	 * Test whether the implementation works as expected when you update the node binary data to an image and back to a non image. The image related fields
	 * should disappear.
	 */
	@Test
	@Ignore("image prop handling not yet implemented")
	public void testUpdateBinaryToImageAndNonImage() {

	}

	@Test
	public void testFileUploadLimit() throws IOException {

		int binaryLen = 10000;
		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(binaryLen - 1);
		String contentType = "application/octet-stream";
		String fileName = "somefile.dat";

		Node node = folder("news");
		prepareSchema(node, "", "binary");

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_error_uploadlimit_reached", "9 KB", "9 KB");
	}

	@Test
	public void testPathSegmentation() throws IOException {
		Node node = folder("news");
		node.setUuid(UUIDUtil.randomUUID());

		// Add some test data
		prepareSchema(node, "", "binary");
		String contentType = "application/octet-stream";
		String fileName = "somefile.dat";
		int binaryLen = 10000;
		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

		// Load the uploaded binary field and return the segment path to the field
		BinaryGraphField binaryField = node.getGraphFieldContainer(english()).getBinary("binary");
		String uuid = "b677504736ed47a1b7504736ed07a14a";
		binaryField.setUuid(uuid);
		String path = binaryField.getSegmentedPath();
		assertEquals("/b677/5047/36ed/47a1/b750/4736/ed07/a14a/", path);
	}

	@Test
	public void testUpload() throws Exception {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		prepareSchema(node, "", "binary");

		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_binary_field_updated", future, node.getUuid());

		node.reload();

		Future<NodeResponse> responseFuture = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
		latchFor(responseFuture);
		assertSuccess(responseFuture);
		NodeResponse response = responseFuture.result();

		BinaryField binaryField = response.getField("binary", BinaryField.class);
		assertEquals("The filename should be set in the response.", fileName, binaryField.getFileName());
		assertEquals("The contentType was correctly set in the response.", contentType, binaryField.getMimeType());
		assertEquals("The binary length was not correctly set in the response.", binaryLen, binaryField.getFileSize());
		assertNotNull("The hashsum was not found in the response.", binaryField.getSha512sum());
		assertNull("The data did not contain image information.", binaryField.getDpi());
		assertNull("The data did not contain image information.", binaryField.getWidth());
		assertNull("The data did not contain image information.", binaryField.getHeight());

		Future<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary");
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