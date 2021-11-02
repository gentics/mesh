package com.gentics.mesh.core.webrootfield;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.parameter.LinkType.MEDIUM;
import static com.gentics.mesh.parameter.LinkType.SHORT;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.AWSTestMode.MINIO;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.*;
import com.gentics.mesh.test.MeshTestSetting;
import org.junit.Assert;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.URIUtils;

@MeshTestSetting(awsContainer = MINIO, testSize = FULL, startServer = true)
public class WebRootFieldEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadFolderByPath() throws Exception {
		HibNode node = folder("2015");
		String nodeUuid = tx(() -> node.getUuid());
		String path = "/News/2015";

		MeshWebrootFieldResponse response = call(
				() -> client().webrootField(PROJECT_NAME, "name", path, new VersioningParametersImpl().draft()));
		assertFalse(response.isBinary());
		Assert.assertEquals("Webroot response node uuid header value did not match", nodeUuid, response.getNodeUuid());
	}

	@Test
	public void testReadFolderContentFieldByPathAndResolveLinks() throws Exception {
		HibNode content = content("news_2015");

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			contentDao.getLatestDraftFieldContainer(content, english()).getHtml("content")
					.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");
			tx.success();
		}

		try (Tx tx = tx()) {
			String path = "/News/2015/News_2015.en.html";
			MeshWebrootFieldResponse restField = call(
					() -> client().webrootField(PROJECT_NAME, "content", path, new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.FULL).setLanguages("en")));
			assertNotNull(restField.getResponseAsPlainText());
			assertFalse(restField.isBinary());
			String contentField = restField.getResponseAsPlainText();
			assertNotNull(contentField);
			assertEquals("Check rendered content",
					"<a href=\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>",
					contentField);
			Assert.assertEquals(restField.getNodeUuid(), content.getUuid());
		}

	}

	@Test
	public void testReadContentFieldByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";

		MeshWebrootFieldResponse restField = call(() -> client().webrootField(PROJECT_NAME, "content", path,
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")));

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = content("news_2015");
			String contentField = restField.getResponseAsPlainText();
			assertEquals(contentDao.getFieldContainer(node, "en").getHtml("content").getHTML(), contentField);
		}
	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();

			HibNode parentNode = folder("2015");
			// Update content schema and add node field
			HibSchema folderSchema = schemaContainer("folder");
			SchemaVersionModel schema = folderSchema.getLatestVersion().getSchema();
			schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
			folderSchema.getLatestVersion().setSchema(schema);
			mesh().serverSchemaStorage().addSchema(schema);

			// Create content which is only german
			HibSchema contentSchema = schemaContainer("content");
			HibNode node = nodeDao.create(parentNode, user(), contentSchema.getLatestVersion(), project());

			// Grant permissions to the node otherwise it will not be able to be loaded
			roleDao.grantPermissions(role(), node, InternalPermission.values());
			HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, german(),
					project().getLatestBranch(), user());
			englishContainer.createString("teaser").setString("german teaser");
			englishContainer.createString("title").setString("german title");
			englishContainer.createString("displayName").setString("german displayName");
			englishContainer.createString("slug").setString("test.de.html");

			// Add node reference to node 2015
			contentDao.getLatestDraftFieldContainer(parentNode, english()).createNode("nodeRef", node);
			tx.success();
		}

		try (Tx tx = tx()) {
			String path = "/News/2015";
			MeshWebrootFieldResponse restNode = call(
					() -> client().webrootField(PROJECT_NAME, "nodeRef", path, new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(MEDIUM).setLanguages("en", "de")));

			NodeResponse node = call(() -> client().findNodeByUuid(PROJECT_NAME, restNode.getNodeUuid(),
					new VersioningParametersImpl().draft(),
					new NodeParametersImpl().setResolveLinks(MEDIUM).setLanguages("en", "de")));

			Assert.assertEquals(JsonUtil.toJson(node.getFields().getNodeField("nodeRef")), restNode.getResponseAsJsonString());

		}

	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		awaitConcurrentRequests(nJobs, i -> client().webrootField(PROJECT_NAME, "content", path,
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")));
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		call(() -> client().webrootField(PROJECT_NAME, "content", path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")));
	}

	/**
	 * Test if a webroot request containing spaces in the path string never returns.
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 2000)
	public void testPathWithSpacesTimeout() throws Exception {
		String path = "/path with spaces";
		call(() -> client().webrootField(PROJECT_NAME, "content", path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test
	public void testPathWithPlus() throws Exception {
		// Test RFC3986 sub-delims and an additional space and questionmark
		String newName = "20!$&'()*+,;=%3F? 15";
		String uuid = tx(() -> folder("2015").getUuid());

		NodeResponse before = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		NodeUpdateRequest nodeUpdateRequest = before.toRequest();
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		NodeResponse after = call(() -> client().findNodeByUuid(PROJECT_NAME, tx(() -> folder("2015").getUuid()),
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		assertEquals("/News/" + URIUtils.encodeSegment(newName), after.getPath());

		String[] path = new String[] { "News", newName };
		MeshWebrootFieldResponse response = call(
				() -> client().webrootField(PROJECT_NAME, "slug", path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setLanguages("en", "de").setResolveLinks(SHORT)));
		Assert.assertEquals(uuid, response.getNodeUuid());
		Assert.assertEquals(newName, response.getResponseAsPlainText());
	}

	@Test
	public void testPathWithSlash() throws Exception {
		String newName = "2015/2016";
		String uuid = tx(() -> folder("2015").getUuid());

		NodeResponse before = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		NodeUpdateRequest nodeUpdateRequest = before.toRequest();
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		NodeResponse after = call(() -> client().findNodeByUuid(PROJECT_NAME, tx(() -> folder("2015").getUuid()),
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		assertEquals("/News/" + URIUtils.encodeSegment(newName), after.getPath());

		String[] path = new String[] { "News", newName };
		MeshWebrootFieldResponse response = call(
				() -> client().webrootField(PROJECT_NAME, "slug", path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setLanguages("en", "de").setResolveLinks(LinkType.SHORT)));
		Assert.assertEquals(uuid, response.getNodeUuid());
		Assert.assertEquals(newName, response.getResponseAsPlainText());
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		call(() -> client().webrootField(PROJECT_NAME, "content", path), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyPath() {
		MeshWebrootFieldResponse response = client().webrootField(PROJECT_NAME, "content", "").blockingGet();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeUuid());
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyField() {
		String newName = "2020";
		String uuid = tx(() -> folder("2015").getUuid());

		NodeResponse before = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid,
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		NodeUpdateRequest nodeUpdateRequest = before.toRequest();
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, uuid, nodeUpdateRequest));

		NodeResponse after = call(() -> client().findNodeByUuid(PROJECT_NAME, tx(() -> folder("2015").getUuid()),
				new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		assertEquals("/News/" + URIUtils.encodeSegment(newName), after.getPath());

		String[] path = new String[] { "News", newName };
		MeshWebrootFieldResponse response = call(
				() -> client().webrootField(PROJECT_NAME, "", path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setLanguages("en", "de").setResolveLinks(LinkType.SHORT)));
		Assert.assertEquals(uuid, response.getNodeUuid());
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g:
		// /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					Map<String, String> languages = new HashMap<String, String>();
					languages.put("en", "english");
					languages.put("de", "german");

					for (Entry<String, String> language : languages.entrySet()) {
						MeshWebrootFieldResponse response = call(() -> client().webrootField(PROJECT_NAME, "title",
								new String[] { path1, path2, path3, name + "." + language.getKey() + ".html" },
								new VersioningParametersImpl().draft()));

						Assert.assertEquals("Check response language", name + " " + language.getValue() + " title",
								response.getResponseAsPlainText());
					}
				}
			}
		}
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "/News/2015";
		String uuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibNode newsFolder = folder("2015");
			uuid = newsFolder.getUuid();
			roleDao.revokePermissions(role(), newsFolder, READ_PERM);
			roleDao.revokePermissions(role(), newsFolder, READ_PUBLISHED_PERM);
			tx.success();
		}

		call(() -> client().webrootField(PROJECT_NAME, "name", englishPath, new VersioningParametersImpl().draft()),
				FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "/News/2015/no-valid-content.html";
		call(() -> client().webrootField(PROJECT_NAME, "name", invalidPath), NOT_FOUND, "node_not_found_for_path",
				invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "/News/no-valid-folder/no-valid-content.html";
		call(() -> client().webrootField(PROJECT_NAME, "name", invalidPath), NOT_FOUND, "node_not_found_for_path",
				invalidPath);
	}

	@Test
	public void testRead404Page() {
		String notFoundPath = "/error/404";
		call(() -> client().webrootField(PROJECT_NAME, "name", notFoundPath), NOT_FOUND, "node_not_found_for_path",
				notFoundPath);
	}

	/**
	 * Test reading the "not found" path /error/404, when this resolves to an
	 * existing node. We expect the node to be returned, but the status code still
	 * to be 404
	 */
	@Test
	public void testRead404Node() {
		String notFoundPath = "/error/404";

		try (Tx tx = tx()) {
			NodeCreateRequest createErrorFolder = new NodeCreateRequest();
			createErrorFolder.setSchema(new SchemaReferenceImpl().setName("folder"));
			createErrorFolder.setParentNodeUuid(project().getBaseNode().getUuid());
			createErrorFolder.getFields().put("slug", FieldUtil.createStringField("error"));
			createErrorFolder.setLanguage("en");
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, createErrorFolder));
			String errorNodeUuid = response.getUuid();

			NodeCreateRequest create404Node = new NodeCreateRequest();
			create404Node.setSchema(new SchemaReferenceImpl().setName("content"));
			create404Node.setParentNodeUuid(errorNodeUuid);
			create404Node.getFields().put("slug", FieldUtil.createStringField("404"));
			create404Node.getFields().put("teaser", FieldUtil.createStringField("Error Content"));
			create404Node.getFields().put("content", FieldUtil.createStringField("An error happened"));
			create404Node.setLanguage("en");
			call(() -> client().createNode(PROJECT_NAME, create404Node));

			call(() -> client().webrootField(PROJECT_NAME, "name", notFoundPath,
					new VersioningParametersImpl().draft()), NOT_FOUND);
		}
	}

	@Test
	public void testWebrootCacheControlPrivateNode() {
		String path = "/News/2015";

		MeshResponse<MeshWebrootFieldResponse> response = client()
				.webrootField(PROJECT_NAME, "name", path, new VersioningParametersImpl().published()).getResponse()
				.blockingGet();
		String cacheControl = response.getHeader("Cache-Control").get();
		assertEquals("private", cacheControl);
	}

	@Test
	public void testWebrootCacheControlPublicNode() {
		String path = "/News/2015";

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(anonymousRole(), folder("2015"), READ_PERM);
			tx.success();
		}

		MeshResponse<MeshWebrootFieldResponse> response = client()
				.webrootField(PROJECT_NAME, "name", path, new VersioningParametersImpl().published()).getResponse()
				.blockingGet();
		String cacheControl = response.getHeader("Cache-Control").get();
		assertEquals("public", cacheControl);
	}

	@Test
	public void testWebrootCacheControlPublicPublishedNode() {
		String path = "/News/2015";

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(anonymousRole(), folder("2015"), READ_PUBLISHED_PERM);
			tx.success();
		}

		MeshResponse<MeshWebrootFieldResponse> response = client()
				.webrootField(PROJECT_NAME, "name", path, new VersioningParametersImpl().published()).getResponse()
				.blockingGet();
		String cacheControl = response.getHeader("Cache-Control").get();
		assertEquals("public", cacheControl);

		// Read again - this time the draft. The anonymous role is not allowed to read
		// this
		response = client().webrootField(PROJECT_NAME, "name", path).getResponse().blockingGet();
		cacheControl = response.getHeader("Cache-Control").get();
		assertEquals("private", cacheControl);
	}

	@Test
	public void testReadPublished() {
		String path = "/News/2015";
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		call(() -> client().takeNodeOffline(PROJECT_NAME, baseNodeUuid,
				new PublishParametersImpl().setRecursive(true)));

		// 1. Assert that published path cannot be found
		call(() -> client().webrootField(PROJECT_NAME, "content", path, new VersioningParametersImpl().published()),
				NOT_FOUND, "node_not_found_for_path", path);

		// 2. Publish nodes
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			BulkActionContext bac = createBulkContext();
			nodeDao.publish(folder("news"), mockActionContext(), bac);
			nodeDao.publish(folder("2015"), mockActionContext(), bac);
			tx.success();
		}

		// 3. Assert that published path can be found
		try (Tx tx = tx()) {
			MeshWebrootFieldResponse restNode = call(
					() -> client().webrootField(PROJECT_NAME, "name", path, new NodeParametersImpl()));
			Assert.assertEquals(restNode.getResponseAsPlainText(), "2015");
		}
	}

	@Test
	public void testReadPublishedWithNoReadPerm() {
		String path = "/News/2015";

		// 1. Publish all nodes
		try (Tx tx = tx()) {
			call(() -> client().publishNode(PROJECT_NAME, project().getBaseNode().getUuid(),
					new PublishParametersImpl().setRecursive(true)));
		}

		// 2. Remove read perm and grant only publish perm to node
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), folder("2015"), READ_PERM);
			roleDao.grantPermissions(role(), folder("2015"), READ_PUBLISHED_PERM);
			tx.success();
		}

		// 3. Assert that published path can be found
		try (Tx tx = tx()) {
			MeshWebrootFieldResponse restNode = call(() -> client().webrootField(PROJECT_NAME, "name", path,
					new NodeParametersImpl(), new VersioningParametersImpl().published()));
			Assert.assertEquals(restNode.getResponseAsPlainText(), "2015");
		}
	}

	@Test
	public void testReadPublishedDifferentFromDraft() {
		String publishedPath = "/News/2015";
		String draftPath = "/News_draft/2015_draft";

		// 1. Publish nodes
		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			BulkActionContext bac = createBulkContext();
			nodeDao.publish(folder("news"), mockActionContext(), bac);
			nodeDao.publish(folder("2015"), mockActionContext(), bac);
		});

		// 2. Change names
		String newsUuid = tx(() -> folder("news").getUuid());
		updateSlug(newsUuid, "en", "News_draft", initialBranchUuid());
		String folder2015Uuid = tx(() -> folder("2015").getUuid());
		updateSlug(folder2015Uuid, "en", "2015_draft", initialBranchUuid());

		// 3. Assert published path in published
		tx(() -> {
			MeshWebrootFieldResponse restNode = call(() -> client().webrootField(PROJECT_NAME, "slug", publishedPath,
					new VersioningParametersImpl().published()));
			Assert.assertEquals(restNode.getResponseAsPlainText(), "2015");
		});

		// 4. Assert published path in draft
		tx(() -> {
			call(() -> client().webrootField(PROJECT_NAME, "slug", publishedPath,
					new VersioningParametersImpl().draft()), NOT_FOUND, "node_not_found_for_path", publishedPath);
		});

		// 5. Assert draft path in draft
		tx(() -> {
			MeshWebrootFieldResponse restNode = call(() -> client().webrootField(PROJECT_NAME, "slug", draftPath,
					new VersioningParametersImpl().draft()));
			Assert.assertEquals(restNode.getResponseAsPlainText(), "2015_draft");
		});

		// 6. Assert draft path in published
		tx(() -> {
			call(() -> client().webrootField(PROJECT_NAME, "slug", draftPath,
					new VersioningParametersImpl().published()), NOT_FOUND, "node_not_found_for_path", draftPath);
		});
	}

	@Test
	public void testReadForBranch() {
		String newBranchName = "newbranch";
		String initialPath = "/News/2015";
		String newPath = "/News_new/2015_new";
		String folder2015Uuid = tx(() -> folder("2015").getUuid());
		String newsUuid = tx(() -> folder("news").getUuid());

		// 1. create new branch and migrate node
		grantAdmin();
		waitForJobs(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setName(newBranchName);
			call(() -> client().createBranch(PROJECT_NAME, branchCreateRequest));
		}, JobStatus.COMPLETED, 1);

		// Assert name in initial branch after migration
		MeshWebrootFieldResponse restNode2 = call(() -> client().webrootField(PROJECT_NAME, "slug", initialPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		Assert.assertEquals(restNode2.getResponseAsPlainText(), "2015");

		// Assert name in new branch after migration
		MeshWebrootFieldResponse restNode3 = call(() -> client().webrootField(PROJECT_NAME, "slug", initialPath,
				new VersioningParametersImpl().draft().setBranch(newBranchName)));
		Assert.assertEquals(restNode3.getResponseAsPlainText(), "2015");

		// 2. update nodes in new branch
		updateSlug(newsUuid, "en", "News_new", newBranchName);
		updateSlug(folder2015Uuid, "en", "2015_new", newBranchName);

		// 3. Assert new name in new branch
		MeshWebrootFieldResponse restNode = call(() -> client().webrootField(PROJECT_NAME, "slug", newPath,
				new VersioningParametersImpl().draft().setBranch(newBranchName)));
		Assert.assertEquals(restNode.getResponseAsPlainText(), "2015_new");

		// 4. Assert new name in initial branch
		call(() -> client().webrootField(PROJECT_NAME, "slug", newPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())), NOT_FOUND,
				"node_not_found_for_path", newPath);

		// 5. Assert old names in initial branch
		MeshWebrootFieldResponse restNode4 = call(() -> client().webrootField(PROJECT_NAME, "slug", initialPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		Assert.assertEquals(restNode4.getResponseAsPlainText(), "2015");

		// 6. Assert old names in new branch
		call(() -> client().webrootField(PROJECT_NAME, "slug", initialPath, new VersioningParametersImpl().draft()),
				NOT_FOUND, "node_not_found_for_path", initialPath);
	}

	/**
	 * Update the node slug field for the latest branch.
	 * 
	 * @param uuid     node uuid
	 * @param language language
	 * @param newName  new name
	 * @param branch
	 */
	protected void updateSlug(String uuid, String language, String newName, String branch) {
		NodeResponse node = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		NodeUpdateRequest update = node.toRequest();
		update.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, uuid, update, new VersioningParametersImpl().setBranch(branch)));
	}

	/**
	 * Update the node name for the given branch.
	 * 
	 * @param node     node
	 * @param branch   branch
	 * @param language language
	 * @param newName  new name
	 */
	protected void updateName(Node node, Branch branch, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update,
				new VersioningParametersImpl().setBranch(branch.getUuid())));
	}
}
