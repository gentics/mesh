package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

public class NodeBinaryVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Before
	public void setup() throws IOException {
		File uploadDir = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory());
		FileUtils.deleteDirectory(uploadDir);
		uploadDir.mkdirs();

		File tempDir = new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory());
		FileUtils.deleteDirectory(tempDir);
		tempDir.mkdirs();

		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);
	}

	@After
	public void cleanup() throws Exception {
		super.cleanup();
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory()));
	}

	private void prepareSchema(Node node, boolean binaryFlag, String contentTypeWhitelist) throws IOException {
		// Update the schema and enable binary support for folders
		Schema schema = node.getSchemaContainer().getSchema();
		schema.setBinary(binaryFlag);
		// schema.set
		// node.getSchemaContainer().setSchema(schema);
	}

	private Future<GenericMessageResponse> uploadFile(Node node, int binaryLen, String contentType, String fileName) throws IOException {

		resetClientSchemaStorage();
		// role().grantPermissions(node, UPDATE_PERM);
		Buffer buffer = TestUtils.randomBuffer(binaryLen);

		return getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), buffer, fileName, contentType);
	}

	@Test
	public void testUploadWithNoPerm() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		prepareSchema(node, true, "");
		role().revokePermissions(node, UPDATE_PERM);

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
	}

	@Test
	public void testUploadWithInvalidMimetype() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";
		Node node;
		node = folder("news");
		prepareSchema(node, false, "image/.*");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_error_no_binary_node");
	}

	@Test
	public void testUploadTwiceToBinaryNode() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		Node node = folder("news");
		prepareSchema(node, true, "");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

		future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);

	}

	@Test
	public void testUploadToNoBinaryNode() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 10000;
		String fileName = "somefile.dat";

		Node node = folder("news");
		prepareSchema(node, false, "");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_error_no_binary_node");
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
		prepareSchema(node, true, "");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_error_uploadlimit_reached", "9 KB", "9 KB");
	}

	@Test
	public void testPathSegmentation() {
		Node node = folder("news");
		node.setUuid(UUIDUtil.randomUUID());
		String uuid = "b677504736ed47a1b7504736ed07a14a";
		node.setUuid(uuid);
		String path = node.getSegmentedPath();
		assertEquals("/b677/5047/36ed/47a1/b750/4736/ed07/a14a/", path);
	}

	@Test
	public void testUpload() throws Exception {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = folder("news");
		prepareSchema(node, true, "");

		Future<GenericMessageResponse> future = uploadFile(node, binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_binary_field_updated", future, node.getUuid());

		node.reload();
		assertEquals(fileName, node.getBinaryFileName());

		Future<NodeResponse> responseFuture = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
		latchFor(responseFuture);
		assertSuccess(responseFuture);
		NodeResponse response = responseFuture.result();

		assertEquals("The filename should be set in the response.", fileName, response.getFileName());
		assertEquals("The contentType was correctly set in the response.", contentType, response.getBinaryProperties().getMimeType());
		assertEquals("The binary length was not correctly set in the response.", binaryLen, response.getBinaryProperties().getFileSize());
		assertNotNull("The hashsum was not found in the response.", response.getBinaryProperties().getSha512sum());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getDpi());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getWidth());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getHeight());

		Future<NodeDownloadResponse> downloadFuture = getClient().downloadBinaryField(PROJECT_NAME, node.getUuid());
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