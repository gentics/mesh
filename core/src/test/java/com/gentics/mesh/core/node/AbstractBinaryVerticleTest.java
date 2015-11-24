package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

public abstract class AbstractBinaryVerticleTest extends AbstractRestVerticleTest {

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

	protected void prepareSchema(Node node, boolean binaryFlag, String contentTypeWhitelist) throws IOException {
		// Update the schema and enable binary support for folders
		Schema schema = node.getSchemaContainer().getSchema();
		schema.setBinary(binaryFlag);
		// schema.set
		// node.getSchemaContainer().setSchema(schema);
	}

	protected Future<GenericMessageResponse> uploadFile(Node node, int binaryLen, String contentType, String fileName) throws IOException {

		resetClientSchemaStorage();
		// role().grantPermissions(node, UPDATE_PERM);
		Buffer buffer = TestUtils.randomBuffer(binaryLen);

		return getClient().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), buffer, fileName, contentType);
	}

}
