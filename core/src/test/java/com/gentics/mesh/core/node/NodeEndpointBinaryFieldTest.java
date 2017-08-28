package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

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

	private Node prepareSchema() throws IOException {
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		return node;

	}
}
