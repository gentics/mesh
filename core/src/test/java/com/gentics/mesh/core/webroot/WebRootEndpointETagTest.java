package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class WebRootEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testResizeImage() throws IOException {
		try (Tx tx = tx()) {
			String path = "/News/2015/blume.jpg";
			Node node = content("news_2015");

			// 1. Transform the node into a binary content
			SchemaContainer container = schemaContainer("binary_content");
			node.setSchemaContainer(container);
			node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
			prepareSchema(node, "image/*", "binary");

			// 2. Upload image
			uploadImage(node, "en", "binary");

			// 3. Resize image
			ImageManipulationParameters params = new ImageManipulationParametersImpl().setWidth(100).setHeight(102);
			MeshResponse<WebRootResponse> response = client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft"))
				.invoke();
			latchFor(response);
			assertSuccess(response);
			String etag = ETag.extract(response.getRawResponse().getHeader(ETAG));
			callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), etag, false, 304);

			params.setHeight(103);
			String newETag = callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), etag,
				false, 200);
			callETag(() -> client().webroot(PROJECT_NAME, path, params, new VersioningParametersImpl().setVersion("draft")), newETag, false, 304);

		}
	}

	@Test
	public void testReadBinaryNode() throws IOException {
		Node node = content("news_2015");
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			// 1. Transform the node into a binary content
			SchemaContainer container = schemaContainer("binary_content");
			node.setSchemaContainer(container);
			node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
			prepareSchema(node, "image/*", "binary");
			tx.success();
		}

		try (Tx tx = tx()) {
			// 2. Update the binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 3. Try to resolve the path
			String path = "/News/2015/somefile.dat";
			MeshResponse<WebRootResponse> response = client()
				.webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setResolveLinks(LinkType.FULL))
				.invoke();

			latchFor(response);
			String etag = ETag.extract(response.getRawResponse().getHeader(ETAG));
			assertNotNull(etag);

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setResolveLinks(LinkType.FULL)), etag, false, 304));

		}

	}

	@Test
	public void testReadOne() {
		String path = "/News/2015/News_2015.en.html";
		try (Tx tx = tx()) {
			Node node = content("news_2015");
			// Inject the reference node field
			SchemaModel schema = node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			node.getGraphFieldContainer("en").getSchemaContainerVersion().setSchema(schema);
			node.getGraphFieldContainer("en").createNode("reference", folder("2015"));
			tx.success();
		}

		MeshResponse<WebRootResponse> response = client()
			.webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")).invoke();
		latchFor(response);

		try (Tx tx = tx()) {
			Node node = content("news_2015");
			String etag = node.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getRawResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			assertEquals(etag, callETag(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")), etag, true, 304));
		}

	}

}
