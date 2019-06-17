package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

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
public class WebRootEndpointPathPrefixTest extends AbstractMeshTest {

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
		call(() -> client().webroot(PROJECT_NAME, "/slugValue"), NOT_FOUND, "node_not_found_for_path", "/slugValue");
		call(() -> client().webroot(PROJECT_NAME, niceUrlPath), NOT_FOUND, "node_not_found_for_path", niceUrlPath);
		call(() -> client().webroot(PROJECT_NAME, "/not_found"), NOT_FOUND, "node_not_found_for_path", "/not_found");

		// Assert that resolving with prefix works
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/" + prefix + "/slugValue"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/" + prefix + niceUrlPath))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/" + prefix + "/some/other/url"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/" + prefix + "/middle"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/" + prefix + "/last/segment"))).hasUuid(uuid);

	}
}
