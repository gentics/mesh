package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class WebRootEndpointUrlPathTest extends AbstractMeshTest {

	@Test
	public void testUrlPathResolving() {

		final String niceUrlPath = "/some/wonderful/short/url";

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setUrlFields("shortUrl");
		request.setName("dummySchema");
		request.setSegmentField("slug");
		request.addField(FieldUtil.createStringFieldSchema("shortUrl"));
		request.addField(FieldUtil.createStringFieldSchema("slug"));

		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField(niceUrlPath));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		WebRootResponse webrootResponse = call(() -> client().webroot(PROJECT_NAME, niceUrlPath));
		System.out.println(webrootResponse.toJson());
	}

	/**
	 * Test list resolving.
	 */
	@Test
	public void testUrlPathListResolving() {

		final String niceUrlPath = "/some/wonderful/short/url";

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setUrlFields("shortUrl", "shortUrlList");
		request.setName("dummySchema");
		request.setSegmentField("slug");
		request.addField(FieldUtil.createStringFieldSchema("shortUrl"));
		request.addField(FieldUtil.createStringFieldSchema("slug"));
		request.addField(FieldUtil.createListFieldSchema("shortUrlList", "string"));

		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField(niceUrlPath));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/last/segment"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();

		assertThat(call(() -> client().webroot(PROJECT_NAME, niceUrlPath))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/middle"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/last/segment"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/slugValue"))).hasUuid(uuid);
		call(() -> client().webroot(PROJECT_NAME, "/not_found"), NOT_FOUND, "node_not_found_for_path", "/not_found");

	}

	/**
	 * Assert that a conflict is detected when creating a node which has a conflicting slugValue/shortUrlValue combination.
	 */
	@Test
	public void testConflictInSameNode() {

	}

	/**
	 * Assert that a conflict is detected when updating a node which causes a conflict with the slug value of the node.
	 */
	@Test
	public void testConflictDueUpdateSameNode() {

	}

	/**
	 * Assert that the webroot resolving still works even if the node has only a url field and no segment field value.
	 */
	@Test
	public void testNodeWithOnlyUrlField() {

	}

	/**
	 * Assert that publishing and taking offline a node which only has a url field (no segment field) works.
	 */
	@Test
	public void testPublishUrlFieldNode() {

	}

	/**
	 * Assert that the short url works also fine when having a node which has different short urls for different languages.
	 */
	@Test
	public void testMultiLanguageFieldHandling() {

	}

}
