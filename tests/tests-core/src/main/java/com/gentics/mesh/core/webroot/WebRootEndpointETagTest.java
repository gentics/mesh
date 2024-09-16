package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebRootEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testResizeImage() throws IOException {
		String path = "/News/2015/blume.jpg";
		HibNode node;
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			node = content("news_2015");

			// 1. Transform the node into a binary content
			prepareSchema(node, "image/*", "binary");
			tx.success();
		}

		// 2. Upload image
		uploadImage(node, "en", "binary");

		// 3. Resize image
		ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
		String etag = callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")));
		callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), etag, false, 304);

		params.setHeight(103);
		String newETag = callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), etag,
			false, 200);
		callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), newETag, false, 304);

	}

	@Test
	public void testReadBinaryNode() throws IOException {
		HibNode node = content("news_2015");
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();

			// 1. Transform the node into a binary content
			prepareSchema(node, "image/*", "binary");
			tx.success();
		}

		// 2. Update the binary data
		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

		// 3. Try to resolve the path
		String path = "/News/2015/somefile.dat";

		String etag = callETag(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
			new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
		assertNotNull(etag);

		// Check whether 304 is returned for correct etag
		assertEquals(etag, callETag(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
			new NodeParametersImpl().setResolveLinks(LinkType.FULL)), etag, false, 304));

	}

	@Test
	public void testReadOne() {
		String path = "/News/2015/News_2015.en.html";

		tx((tx) -> {
			ContentDao contentDao = tx.contentDao();
			HibNode node = content("news_2015");
			// Inject the reference node field
			HibNodeFieldContainer original = contentDao.getFieldContainer(node, "en");
			HibSchemaVersion schemaVersion = contentDao.getSchemaContainerVersion(original);
			SchemaVersionModel schema = schemaVersion.getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			schemaVersion.setSchema(schema);
			actions().updateSchemaVersion(schemaVersion);
		});

		try (Tx tx = tx()) {
			NodeResponse response = call(() -> client().findNodeByUuid(projectName(), content("news_2015").getUuid()));
			NodeUpdateRequest request = response.toRequest();
			request.getFields()
					.put("reference", new NodeFieldImpl().setUuid(folder("2015").getUuid()));
			call(() -> client().updateNode(projectName(), content("news_2015").getUuid(), request));
			tx.success();
		}

		String responseTag = callETag(
			() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")));

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibNode node = content("news_2015");
			String etag = nodeDao.getETag(node, mockActionContext());
			assertEquals(etag, responseTag);

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")), etag, true, 304));
		}

	}

}
