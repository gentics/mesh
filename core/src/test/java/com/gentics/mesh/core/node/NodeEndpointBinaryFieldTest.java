package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;
import rx.Observable;
import rx.functions.Func1;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeEndpointBinaryFieldTest extends AbstractMeshTest {

	@Test
	public void testDownloadBinaryFieldWithReadPublishPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		// Only grant read_published perm
		try (Tx tx = tx()) {
			role().revokePermissions(node, READ_PERM);
			role().grantPermissions(node, READ_PUBLISHED_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 1. Upload some binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
			call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
			// 2. Download the data using the REST API
			NodeDownloadResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary",
					new VersioningParametersImpl().setVersion("published")));
			assertEquals(binaryLen, response.getBuffer().length());
		}
	}

	@Test
	public void testDownloadBinaryFieldDraftWithNoReadPublishPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		// Only grant read_published perm
		try (Tx tx = tx()) {
			role().revokePermissions(node, READ_PERM);
			role().grantPermissions(node, READ_PUBLISHED_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 1. Upload some binary data - This will update the draft
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 2. Download the data using the REST API
			call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary",
					new VersioningParametersImpl().setVersion("draft")), FORBIDDEN, "error_missing_perm", node.getUuid());

		}
	}

	@Test
	public void testDownloadBinaryField() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		try (Tx tx = tx()) {
			// 1. Upload some binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 2. Download the data using the REST API
			NodeDownloadResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary"));
			assertEquals(binaryLen, response.getBuffer().length());
		}
	}

	@Test
	public void testUploadImagesConcurrently() throws IOException {
		String parentUuid;
		try (Tx tx = tx()) {
			Node node = folder("2015");
			parentUuid = node.getUuid();
			tx.success();
		}

		// Create schema with 2 binary fields
		SchemaCreateRequest schemaRequest = new SchemaCreateRequest()
			.setName("imageSchema")
			.setFields(Arrays.asList(
				new BinaryFieldSchemaImpl()
					.setName("image1"),
				new BinaryFieldSchemaImpl()
					.setName("image2")
					.setRequired(true)
			));

		SchemaResponse schema = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// Create node of that new schema
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest()
			.setSchema(schema.toReference())
			.setParentNodeUuid(parentUuid)
			.setLanguage("en");


		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		// Load the image from the file system
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		Func1<String, Observable<NodeResponse>> uploadBinary = (fieldName) ->
			client().updateNodeBinaryField(PROJECT_NAME, nodeResponse.getUuid(), nodeResponse.getLanguage(),
            nodeResponse.getVersion(), fieldName, buffer, "blume.jpg", "image/jpeg").toObservable();

		// Upload 2 images at once
		// This should work since we can update the same node at the same time if it affects different fields
		Observable.just("image1", "image2")
			.flatMap(uploadBinary)
			.toCompletable().await();
	}

	private Node prepareSchema() throws IOException {
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		return node;

	}
}
