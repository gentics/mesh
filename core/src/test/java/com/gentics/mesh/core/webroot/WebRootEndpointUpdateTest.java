package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import com.gentics.mesh.rest.client.MeshWebrootResponse;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebRootEndpointUpdateTest extends AbstractMeshTest {

	@Test
	public void testCreateNodeViaPath() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		NodeResponse response = call(() -> client().webrootCreate(PROJECT_NAME, "/new-page.html", nodeCreateRequest));
		assertEquals("0.1", response.getVersion());
	}

	@Test
	public void testCreateNodeViaPathAndPrefix() {

		String prefix = "some/prefix";

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setPathPrefix(prefix);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		NodeResponse response = call(() -> client().webrootCreate(PROJECT_NAME, "/" + prefix + "/new-page.html", nodeCreateRequest));
		assertEquals("0.1", response.getVersion());
	}

	@Test
	public void testCreateNodeWithMissingPrefix() {

		String prefix = "some/prefix";

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setPathPrefix(prefix);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/2015/new-page.html";
		call(() -> client().webrootCreate(PROJECT_NAME, path, nodeCreateRequest), NOT_FOUND, "webroot_error_prefix_invalid", path, prefix);

	}

	@Test
	public void testCreateNodeViaSubPath() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("newContent.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/2015/newContent.html";
		NodeResponse response = call(() -> client().webrootCreate(PROJECT_NAME, path, nodeCreateRequest));
		assertEquals("0.1", response.getVersion());
	}

	/**
	 * Assert that creation of a node fails if the request uri segment does not match up with the segment which was included in the json body.
	 */
	@Test
	public void testCreateNodeViaSubPathSegmentMismatch() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/2015/newContent.html";
		call(() -> client().webrootCreate(PROJECT_NAME, path, nodeCreateRequest), BAD_REQUEST, "webroot_error_segment_field_mismatch",
			"newContent.html", "new-page.html");

	}

	@Test
	public void testMissingParentSegmentOnUpdate() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/missing/News_2015.en.html";
		call(() -> client().webrootUpdate(PROJECT_NAME, path, nodeUpdateRequest), NOT_FOUND, "webroot_error_parent_not_found", "/News");
	}

	@Test
	public void testMissingParentSegmentOnCreate() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/missing/News_2015.en.html";
		String errorPath = "/News";
		call(() -> client().webrootCreate(PROJECT_NAME, path, nodeCreateRequest), NOT_FOUND, "webroot_error_parent_not_found", errorPath);
	}

	@Test
	public void testUpdateNodeViaPath() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/2015/News_2015.en.html";
		NodeResponse response = call(() -> client().webrootUpdate(PROJECT_NAME, path, nodeUpdateRequest));
		System.out.println(response.toJson());
	}

	@Test
	public void testUpdateNodeViaPathAndPrefix() {
		String prefix = "some/prefix";

		BranchUpdateRequest request = new BranchUpdateRequest();
		request.setPathPrefix(prefix);
		call(() -> client().updateBranch(PROJECT_NAME, initialBranchUuid(), request));

		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		call(() -> client().webrootUpdate(PROJECT_NAME, "/" + prefix + "/News/2015/News_2015.en.html", nodeUpdateRequest));

		String withoutPrefix = "/News/2015/News_2015.en.html";
		call(() -> client().webrootUpdate(PROJECT_NAME, withoutPrefix, nodeUpdateRequest), NOT_FOUND, "webroot_error_prefix_invalid",
			withoutPrefix, prefix);
	}

	@Test
	public void testUpdateNodeSegmentViaPath() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));

		String path = "/News/2015/News_2015.en.html";
		NodeResponse response = call(() -> client().webrootUpdate(PROJECT_NAME, path, nodeUpdateRequest));
		System.out.println(response.toJson());

		MeshWebrootResponse checkResponse = call(() -> client().webroot(PROJECT_NAME, "/News/2015/new-page.html"));
		assertEquals("The same node (uuid) should have been renamed", response.getUuid(), checkResponse.getNodeResponse().getUuid());
		assertEquals("The same node (lang) should have been renamed", response.getLanguage(), checkResponse.getNodeResponse().getLanguage());
	}

	@Test
	public void testAddLanguageContentViaPath() {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("nl");
		nodeUpdateRequest.setVersion("1.0");
		nodeUpdateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser nl"));
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.nl.html"));
		nodeUpdateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime in nl!"));

		String path = "/News/2015/News_2015.en.html";
		NodeResponse response = call(() -> client().webrootUpdate(PROJECT_NAME, path, nodeUpdateRequest));
		System.out.println(response.toJson());

		MeshWebrootResponse checkResponse = call(() -> client().webroot(PROJECT_NAME, "/News/2015/new-page.nl.html"));
		assertEquals("The same node (uuid) should have been renamed", response.getUuid(), checkResponse.getNodeResponse().getUuid());
		assertEquals("nl", checkResponse.getNodeResponse().getLanguage());
	}
}
