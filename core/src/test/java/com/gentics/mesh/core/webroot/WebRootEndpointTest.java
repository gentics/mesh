package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.expectFailureMessage;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.URIUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class WebRootEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadBinaryNode() throws IOException {
		Node node = content("news_2015");
		try (Tx tx = tx()) {
			// 1. Transform the node into a binary content
			SchemaContainer container = schemaContainer("binary_content");
			node.setSchemaContainer(container);
			node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
			prepareSchema(node, "image/*", "binary");
			tx.success();
		}

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		try (Tx tx = tx()) {
			// 2. Update the binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 3. Try to resolve the path
			String path = "/News/2015/somefile.dat";
			WebRootResponse response = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
					new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
			NodeDownloadResponse downloadResponse = response.getDownloadResponse();
			assertTrue(response.isDownload());
			assertNotNull(downloadResponse);
		}
	}

	@Test
	public void testReadFolderByPath() throws Exception {
		try (Tx tx = tx()) {
			Node folder = folder("2015");
			String path = "/News/2015";

			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft()));
			assertThat(restNode.getNodeResponse()).is(folder).hasLanguage("en");
		}
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		Node content = content("news_2015");

		try (Tx tx = tx()) {
			content.getLatestDraftFieldContainer(english()).getHtml("content")
					.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");
			tx.success();
		}

		try (Tx tx = tx()) {
			String path = "/News/2015/News_2015.en.html";
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
					new NodeParametersImpl().setResolveLinks(LinkType.FULL).setLanguages("en")));
			HtmlFieldImpl contentField = restNode.getNodeResponse().getFields().getHtmlField("content");
			assertNotNull(contentField);
			assertEquals("Check rendered content", "<a href=\"/api/v1/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>",
					contentField.getHTML());
			assertThat(restNode.getNodeResponse()).is(content).hasLanguage("en");
		}

	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";

		WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")));

		try (Tx tx = tx()) {
			Node node = content("news_2015");
			assertThat(restNode.getNodeResponse()).is(node).hasLanguage("en");
		}
	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {

		try (Tx tx = tx()) {
			Node parentNode = folder("2015");
			// Update content schema and add node field
			SchemaContainer folderSchema = schemaContainer("folder");
			SchemaModel schema = folderSchema.getLatestVersion().getSchema();
			schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
			folderSchema.getLatestVersion().setSchema(schema);
			MeshInternal.get().serverSchemaStorage().addSchema(schema);

			// Create content which is only german
			SchemaContainer contentSchema = schemaContainer("content");
			Node node = parentNode.create(user(), contentSchema.getLatestVersion(), project());

			// Grant permissions to the node otherwise it will not be able to be loaded
			role().grantPermissions(node, GraphPermission.values());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(german(), project().getLatestRelease(), user());
			englishContainer.createString("teaser").setString("german teaser");
			englishContainer.createString("title").setString("german title");
			englishContainer.createString("displayName").setString("german displayName");
			englishContainer.createString("slug").setString("test.de.html");

			// Add node reference to node 2015
			parentNode.getLatestDraftFieldContainer(english()).createNode("nodeRef", node);
			tx.success();
		}

		try (Tx tx = tx()) {
			String path = "/News/2015";
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
					new NodeParametersImpl().setResolveLinks(LinkType.MEDIUM).setLanguages("en", "de")));
			assertEquals("The node reference did not point to the german node.", "/dummy/News/2015/test.de.html",
					restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
			assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

			// Again with no german fallback option (only english)
			restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
					new NodeParametersImpl().setResolveLinks(LinkType.MEDIUM).setLanguages("en")));
			assertEquals("The node reference did not point to the 404 path.", "/dummy/error/404",
					restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
			assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());
		}

	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		List<MeshResponse<WebRootResponse>> futures = new ArrayList<>();
		for (int i = 0; i < nJobs; i++) {
			futures.add(client()
					.webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")).invoke());
		}

		for (MeshResponse<WebRootResponse> fut : futures) {
			latchFor(fut);
			assertSuccess(fut);
		}
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")));
	}

	/**
	 * Test if a webroot request containing spaces in the path string never returns.
	 * @throws Exception
	 */
	@Test(timeout=2000)
	public void testPathWithSpacesTimeout() throws Exception {
		String path = "/path with spaces";
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test
	public void testPathWithPlus() throws Exception {
		// Test RFC3986 sub-delims and an additional space and questionmark
		String newName = "20!$&'()*+,;=%3F? 15";
		String uuid = db().tx(() -> folder("2015").getUuid());
		try (Tx tx = tx()) {
			Node folder = folder("2015");
			folder.getGraphFieldContainer("en").getString("slug").setString(newName);
			tx.success();
		}

		String[] path = new String[] { "News", newName };
		WebRootResponse response = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de").setResolveLinks(LinkType.SHORT)));
		assertEquals(uuid, response.getNodeResponse().getUuid());
		assertEquals("/News/" + URIUtils.encodeSegment(newName), response.getNodeResponse().getPath());
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		call(() -> client().webroot(PROJECT_NAME, path), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyPath() {
		MeshResponse<WebRootResponse> future = client().webroot(PROJECT_NAME, "").invoke();
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadProjectBaseNode() {
		WebRootResponse response = call(() -> client().webroot(PROJECT_NAME, "/", new VersioningParametersImpl().draft()));
		assertFalse(response.isDownload());
		try (Tx tx = tx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadDoubleSlashes() {
		WebRootResponse response = call(() -> client().webroot(PROJECT_NAME, "//", new VersioningParametersImpl().draft()));
		assertFalse(response.isDownload());
		try (Tx tx = tx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g: /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					for (String language : Arrays.asList("en", "de")) {
						WebRootResponse response = call(() -> client().webroot(PROJECT_NAME,
								new String[] { path1, path2, path3, name + "." + language + ".html" }, new VersioningParametersImpl().draft()));

						assertEquals("Check response language", language, response.getNodeResponse().getLanguage());
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
			Node newsFolder = folder("2015");
			uuid = newsFolder.getUuid();
			role().revokePermissions(newsFolder, READ_PERM);
			role().revokePermissions(newsFolder, READ_PUBLISHED_PERM);
			tx.success();
		}

		call(() -> client().webroot(PROJECT_NAME, englishPath, new VersioningParametersImpl().draft()), FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "/News/2015/no-valid-content.html";
		call(() -> client().webroot(PROJECT_NAME, invalidPath), NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "/News/no-valid-folder/no-valid-content.html";
		call(() -> client().webroot(PROJECT_NAME, invalidPath), NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testRead404Page() {
		String notFoundPath = "/error/404";
		call(() -> client().webroot(PROJECT_NAME, notFoundPath), NOT_FOUND, "node_not_found_for_path", notFoundPath);
	}

	/**
	 * Test reading the "not found" path /error/404, when this resolves to an existing node. We expect the node to be returned, but the status code still to be
	 * 404
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

			MeshResponse<WebRootResponse> webrootFuture = client().webroot(PROJECT_NAME, notFoundPath, new VersioningParametersImpl().draft())
					.invoke();
			latchFor(webrootFuture);
			expectFailureMessage(webrootFuture, NOT_FOUND, null);
		}
	}

	@Test
	public void testReadPublished() {
		String path = "/News/2015";

		try (Tx tx = tx()) {
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));
		}
		// 1. Assert that published path cannot be found
		try (Tx tx = tx()) {
			call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().published()), NOT_FOUND, "node_not_found_for_path", path);
		}

		// 2. Publish nodes
		try (Tx tx = tx()) {
			BulkActionContext bac = createBulkContext();
			folder("news").publish(mockActionContext(), bac);
			folder("2015").publish(mockActionContext(), bac);
			tx.success();
		}

		// 3. Assert that published path can be found
		try (Tx tx = tx()) {
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new NodeParametersImpl()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("2.0").hasLanguage("en");
		}
	}

	@Test
	public void testReadPublishedWithNoReadPerm() {
		String path = "/News/2015";

		// 1. Publish all nodes
		try (Tx tx = tx()) {
			call(() -> client().publishNode(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));
		}

		// 2. Remove read perm and grant only publish perm to node
		try (Tx tx = tx()) {
			role().revokePermissions(folder("2015"), READ_PERM);
			role().grantPermissions(folder("2015"), READ_PUBLISHED_PERM);
			tx.success();
		}

		// 3. Assert that published path can be found
		try (Tx tx = db().tx()) {
			WebRootResponse restNode = call(
					() -> client().webroot(PROJECT_NAME, path, new NodeParametersImpl(), new VersioningParametersImpl().published()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
		}
	}

	@Test
	public void testReadPublishedDifferentFromDraft() {
		String publishedPath = "/News/2015";
		String draftPath = "/News_draft/2015_draft";

		// 1. Publish nodes
		db().tx(() -> {
			BulkActionContext bac = createBulkContext();
			folder("news").publish(mockActionContext(), bac);
			folder("2015").publish(mockActionContext(), bac);
		});

		// 2. Change names
		db().tx(() -> {
			updateSlug(folder("news"), "en", "News_draft");
			updateSlug(folder("2015"), "en", "2015_draft");
		});

		// 3. Assert published path in published
		db().tx(() -> {
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, publishedPath, new VersioningParametersImpl().published()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
		});

		// 4. Assert published path in draft
		db().tx(() -> {
			call(() -> client().webroot(PROJECT_NAME, publishedPath, new VersioningParametersImpl().draft()), NOT_FOUND, "node_not_found_for_path",
					publishedPath);
		});

		// 5. Assert draft path in draft
		db().tx(() -> {
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, draftPath, new VersioningParametersImpl().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
		});

		// 6. Assert draft path in published
		db().tx(() -> {
			call(() -> client().webroot(PROJECT_NAME, draftPath, new VersioningParametersImpl().published()), NOT_FOUND, "node_not_found_for_path",
					draftPath);
		});
	}

	@Test
	public void testReadForRelease() {
		String newReleaseName = "newrelease";
		String initialPath = "/News/2015";
		String newPath = "/News_new/2015_new";

		// 1. create new release and migrate node
		db().tx(() -> {
			Release newRelease = project().getReleaseRoot().create(newReleaseName, user());
			meshDagger().releaseMigrationHandler().migrateRelease(newRelease, null);
		});

		// 2. update nodes in new release
		db().tx(() -> {
			updateSlug(folder("news"), "en", "News_new");
			updateSlug(folder("2015"), "en", "2015_new");
		});

		// 3. Assert new names in new release
		db().tx(() -> {
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, newPath, new VersioningParametersImpl().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
		});

		// 4. Assert new names in initial release
		db().tx(() -> {
			call(() -> client().webroot(PROJECT_NAME, newPath,
					new VersioningParametersImpl().draft().setRelease(project().getInitialRelease().getUuid())), NOT_FOUND, "node_not_found_for_path",
					newPath);
		});

		// 5. Assert old names in initial release
		db().tx(() -> {
			WebRootResponse restNode = call(() -> client().webroot(PROJECT_NAME, initialPath,
					new VersioningParametersImpl().draft().setRelease(project().getInitialRelease().getUuid())));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
		});

		// 6. Assert old names in new release
		db().tx(() -> {
			call(() -> client().webroot(PROJECT_NAME, initialPath, new VersioningParametersImpl().draft()), NOT_FOUND, "node_not_found_for_path",
					initialPath);
		});
	}

	/**
	 * Update the node slug field for the latest release.
	 * 
	 * @param node
	 *            node
	 * @param language
	 *            language
	 * @param newName
	 *            new name
	 */
	protected void updateSlug(Node node, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.setVersion(node.getGraphFieldContainer(language).getVersion().toString());
		update.getFields().put("slug", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update));
	}

	/**
	 * Update the node name for the given release
	 * 
	 * @param node
	 *            node
	 * @param release
	 *            release
	 * @param language
	 *            language
	 * @param newName
	 *            new name
	 */
	protected void updateName(Node node, Release release, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update, new VersioningParametersImpl().setRelease(release.getUuid())));
	}
}
