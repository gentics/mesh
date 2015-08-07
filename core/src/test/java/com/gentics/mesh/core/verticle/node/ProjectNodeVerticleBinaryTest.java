package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectNodeVerticleBinaryTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testUploadToNoBinaryNode() {
		//TODO expect error
	}

	/**
	 * Test whether the implementation works as expected when you update the node binary data to an image and back to a non image. The image related fields
	 * should disappear.
	 */
	@Test
	public void testUpdateBinaryToImageAndNonImage() {

	}

	@Test
	public void testUpload() throws Exception {

		Node node = folder("news");

		// Update the schema and enable binary support for folders
		Schema schema = node.getSchemaContainer().getSchema();
		schema.setBinary(true);
		node.getSchemaContainer().setSchema(schema);

		resetClientSchemaStorage();
		role().addPermissions(node, UPDATE_PERM);
		int binaryLen = 10000;
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		String fileName = "somefile.dat";
		String contentType = "application/octet-stream";

		Future<GenericMessageResponse> future = getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), buffer, fileName, contentType);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_binary_field_updated", future, node.getUuid());

		Future<NodeResponse> responseFuture = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
		latchFor(responseFuture);
		assertSuccess(responseFuture);
		NodeResponse response = responseFuture.result();

		assertEquals("The filename should be set in the response.", fileName, response.getFileName());
		assertEquals(contentType, response.getBinaryProperties().getMimeType());
		assertEquals(binaryLen, response.getBinaryProperties().getFileSize());
		//TODO add sha512sum support
//		assertNotNull(response.getBinaryProperties().getSha512sum());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getDpi());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getWidth());
		assertNull("The data did not contain image information.", response.getBinaryProperties().getHeight());
	}

}