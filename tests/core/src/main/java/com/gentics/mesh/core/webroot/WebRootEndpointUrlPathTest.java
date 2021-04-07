package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebRootEndpointUrlPathTest extends AbstractMeshTest {

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
	public void testUrlPathResolving() {

		final String niceUrlPath = "/some/wonderful/short/url";

		setupSchema(true);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField(niceUrlPath));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();

		assertThat(call(() -> client().webroot(PROJECT_NAME, niceUrlPath))).hasUuid(uuid);

		// Now verify that no published node can be found
		call(() -> client().webroot(PROJECT_NAME, niceUrlPath, new VersioningParametersImpl().published()), NOT_FOUND, "node_not_found_for_path",
				niceUrlPath);
	}

	/**
	 * Test list resolving.
	 */
	@Test
	public void testUrlPathListResolving() {

		final String niceUrlPath = "/some/wonderful/short/url";

		setupSchema(true);

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
	 * Assert that no problems occur when saving a node which has multiple url fields which share the same value.
	 */
	@Test
	public void testDuplicateFieldValueInSameNode() {

		setupSchema(true);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);

	}

	/**
	 * Assert that a conflict is detected when updating a node which causes a conflict with the url fields value of the second node.
	 */
	@Test
	public void testConflictWithExistingNode() {

		setupSchema(true);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);

		NodeCreateRequest nodeCreateRequest2 = new NodeCreateRequest();
		nodeCreateRequest2.setSchemaName("dummySchema");
		nodeCreateRequest2.setLanguage("en");
		nodeCreateRequest2.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest2.getFields().put("slug", FieldUtil.createStringField("slugValue2"));
		nodeCreateRequest2.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest2.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url2", "/middle3", "/some/other/url4"));
		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest2), CONFLICT, "node_conflicting_urlfield_update", "/some/other/url", uuid,
				"en");

	}

	@Test
	public void testConflictDuringPublish() {
		setupSchema(true);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);

		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// Update the draft values in order to prevent a conflict when creating the second node
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion(nodeResponse.getVersion());
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue2"));
		nodeUpdateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url2"));
		nodeUpdateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url2", "/middle2", "/some/other/url2"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		// Now create the second node
		NodeCreateRequest nodeCreateRequest2 = new NodeCreateRequest();
		nodeCreateRequest2.setSchemaName("dummySchema");
		nodeCreateRequest2.setLanguage("en");
		nodeCreateRequest2.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest2.getFields().put("slug", FieldUtil.createStringField("slugValue1"));
		nodeCreateRequest2.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url1"));
		nodeCreateRequest2.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url1", "/middle", "/some/other/url"));
		NodeResponse secondNode = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest2));

		// Now publish the second node - The published fields of the second node should cause a conflict with the first published node
		call(() -> client().publishNode(PROJECT_NAME, secondNode.getUuid()), CONFLICT, "node_conflicting_urlfield_update", "/some/other/url,/middle",
				uuid, "en");

	}

	/**
	 * Assert that the webroot resolving still works even if the node has only a url field and no segment field value.
	 */
	@Test
	public void testNodeWithOnlyUrlField() {
		setupSchema(false);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
	}

	/**
	 * Assert that publishing and taking offline a node which only has a url field (no segment field) works.
	 */
	@Test
	public void testPublishUrlFieldNode() {
		setupSchema(false);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();

		// Publish it and test with takeOffline (single language)
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url", new VersioningParametersImpl().published()))).hasUuid(uuid);
		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, uuid, "en"));
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
		call(() -> client().webroot(PROJECT_NAME, "/some/other/url", new VersioningParametersImpl().published()), NOT_FOUND,
				"node_not_found_for_path", "/some/other/url");

		// Publish it again and test with takeOffline (all languages)
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url", new VersioningParametersImpl().published()))).hasUuid(uuid);
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid));
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);
		call(() -> client().webroot(PROJECT_NAME, "/some/other/url", new VersioningParametersImpl().published()), NOT_FOUND,
				"node_not_found_for_path", "/some/other/url");

	}

	/**
	 * Assert that the short url works also fine when having a node which has different short urls for different languages.
	 */
	@Test
	public void testMultiLanguageFieldHandling() {

		setupSchema(false);

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName("dummySchema");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeCreateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url"));
		nodeCreateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url", "/middle", "/some/other/url"));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String uuid = nodeResponse.getUuid();

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("de");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeUpdateRequest.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url2"));
		nodeUpdateRequest.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url2", "/middle2", "/some/other/url2"));
		NodeResponse updateResponse = call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url2"))).hasUuid(uuid);
		assertThat(call(() -> client().webroot(PROJECT_NAME, "/some/other/url"))).hasUuid(uuid);

		// Now update [de] again and assert that a conflict with [en] is detected

		NodeUpdateRequest nodeUpdateRequest2 = new NodeUpdateRequest();
		nodeUpdateRequest2.setLanguage("de");
		nodeUpdateRequest2.setVersion(updateResponse.getVersion());
		nodeUpdateRequest2.getFields().put("slug", FieldUtil.createStringField("slugValue"));
		nodeUpdateRequest2.getFields().put("shortUrl", FieldUtil.createStringField("/some/other/url2"));
		// Conflict with /middle
		nodeUpdateRequest2.getFields().put("shortUrlList", FieldUtil.createStringListField("/some/other/url2", "/middle", "/some/other/url2"));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest2), CONFLICT, "node_conflicting_urlfield_update", "/middle", uuid, "en");

	}

}
