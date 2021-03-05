package com.gentics.mesh.core.webrootfield;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebRootFieldEndpointPathPrefixTest extends AbstractMeshTest {

	private void setupSchema(boolean addSegmentField) {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setUrlFields("shortUrl", "shortUrlList");
		request.setName("dummySchema");
		if (addSegmentField) {
			request.setSegmentField("slug");
		}
		request.addField(FieldUtil.createStringFieldSchema("slug"));
		request.addField(FieldUtil.createStringFieldSchema("shortUrl"));
		request.addField(FieldUtil.createListFieldSchema("shortUrlList", "string"));

		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));
	}

	@Test
	public void testPathPrefixWebroot() {
		final String niceUrlPath = "/some/wonderful/short/url";
		final String prefix = "my/prefix";

		setupSchema(true);

		String branchUuid = tx(() -> latestBranch().getUuid());

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setPathPrefix(prefix);
		call(() -> client().updateBranch(PROJECT_NAME, branchUuid, request));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField(niceUrlPath));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/last/segment"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();

		// Assert that path without the prefix can no longer be resolved
		call(() -> client().webrootField(PROJECT_NAME, "slug", "/slugValue"), NOT_FOUND, "node_not_found_for_path", "/slugValue");
		call(() -> client().webrootField(PROJECT_NAME, "slug", niceUrlPath), NOT_FOUND, "node_not_found_for_path", niceUrlPath);
		call(() -> client().webrootField(PROJECT_NAME, "slug", "/not_found"), NOT_FOUND, "node_not_found_for_path", "/not_found");

		// Assert that resolving with prefix works
		assertEquals(call(() -> client().webrootField(PROJECT_NAME, "slug", "/" + prefix + "/slugValue")).getResponseAsPlainText(), "slugValue");
		assertEquals(call(() -> client().webrootField(PROJECT_NAME, "slug", "/" + prefix + niceUrlPath)).getResponseAsPlainText(), "slugValue");
		assertEquals(call(() -> client().webrootField(PROJECT_NAME, "slug", "/" + prefix + "/some/other/url")).getResponseAsPlainText(), "slugValue");
		assertEquals(call(() -> client().webrootField(PROJECT_NAME, "slug", "/" + prefix + "/middle")).getResponseAsPlainText(), "slugValue");
		assertEquals(call(() -> client().webrootField(PROJECT_NAME, "slug", "/" + prefix + "/last/segment")).getResponseAsPlainText(), "slugValue");

	}
}
