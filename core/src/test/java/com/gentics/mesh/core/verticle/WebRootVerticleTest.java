package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.node.AbstractBinaryVerticleTest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.WebRootResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.TakeOfflineParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;

public class WebRootVerticleTest extends AbstractBinaryVerticleTest {

	@Autowired
	private WebRootVerticle webrootVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Autowired
	private NodeMigrationHandler nodeMigrationHandler;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(webrootVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testReadBinaryNode() throws IOException {
		Node node = content("news_2015");

		// 1. Transform the node into a binary content
		SchemaContainer container = schemaContainer("binary-content");
		node.setSchemaContainer(container);
		node.getLatestDraftFieldContainer(english()).setSchemaContainerVersion(container.getLatestVersion());
		prepareSchema(node, "image/*", "binary");
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		// 2. Update the binary data
		Future<GenericMessageResponse> future = updateBinaryField(node, "en", "binary", binaryLen, contentType, fileName);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "node_binary_field_updated", node.getUuid());

		// 3. Try to resolve the path
		String path = "/News/2015/somefile.dat";
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
				new NodeParameters().setResolveLinks(LinkType.FULL)));
		NodeDownloadResponse downloadResponse = response.getDownloadResponse();
		assertTrue(response.isBinary());
		assertNotNull(downloadResponse);

	}

	@Test
	public void testReadFolderByPath() throws Exception {
		Node folder = folder("2015");
		String path = "/News/2015";

		WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft()));
		assertThat(restNode.getNodeResponse()).is(folder).hasLanguage("en");
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		Node content = content("news_2015");

		content.getLatestDraftFieldContainer(english()).getHtml("content")
				.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");

		String path = "/News/2015/News_2015.en.html";
		WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
				new NodeParameters().setResolveLinks(LinkType.FULL).setLanguages("en")));
		HtmlFieldImpl contentField = restNode.getNodeResponse().getFields().getHtmlField("content");
		assertNotNull(contentField);
		assertEquals("Check rendered content", "<a href=\"/api/v1/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>", contentField.getHTML());
		assertThat(restNode.getNodeResponse()).is(content).hasLanguage("en");
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";
		WebRootResponse restNode = call(
				() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de")));

		Node node = content("news_2015");
		assertThat(restNode.getNodeResponse()).is(node).hasLanguage("en");
	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {

		Node parentNode = folder("2015");
		// Update content schema and add node field
		SchemaContainer folderSchema = schemaContainer("folder");
		Schema schema = folderSchema.getLatestVersion().getSchema();
		schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
		folderSchema.getLatestVersion().setSchema(schema);
		ServerSchemaStorage.getInstance().addSchema(schema);

		// Create content which is only german
		SchemaContainer contentSchema = schemaContainer("content");
		Node node = parentNode.create(user(), contentSchema.getLatestVersion(), project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(german(), project().getLatestRelease(), user());
		englishContainer.createString("name").setString("german_name");
		englishContainer.createString("title").setString("german title");
		englishContainer.createString("displayName").setString("german displayName");
		englishContainer.createString("filename").setString("test.de.html");

		// Add node reference to node 2015
		parentNode.getLatestDraftFieldContainer(english()).createNode("nodeRef", node);

		String path = "/News/2015";
		WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
				new NodeParameters().setResolveLinks(LinkType.MEDIUM).setLanguages("en", "de")));
		assertEquals("The node reference did not point to the german node.", "/dummy/News/2015/test.de.html",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

		// Again with no german fallback option (only english)
		restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(),
				new NodeParameters().setResolveLinks(LinkType.MEDIUM).setLanguages("en")));
		assertEquals("The node reference did not point to the 404 path.", "/dummy/error/404",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015", restNode.getNodeResponse().getFields().getStringField("name").getString());

	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		List<Future<WebRootResponse>> futures = new ArrayList<>();
		for (int i = 0; i < nJobs; i++) {
			futures.add(getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de")));
		}

		for (Future<WebRootResponse> fut : futures) {
			latchFor(fut);
			assertSuccess(fut);
		}
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		call(() -> getClient().webroot(PROJECT_NAME, path, new VersioningParameters().draft(), new NodeParameters().setLanguages("en", "de")));
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		call(() -> getClient().webroot(PROJECT_NAME, path), NOT_FOUND, "node_not_found_for_path", path);
	}

	@Test(expected = RuntimeException.class)
	public void testReadWithEmptyPath() {
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, "");
		latchFor(future);
		assertSuccess(future);
		WebRootResponse response = future.result();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadProjectBaseNode() {
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, "/", new VersioningParameters().draft()));
		assertFalse(response.isBinary());
		assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadDoubleSlashes() {
		WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME, "//", new VersioningParameters().draft()));
		assertFalse(response.isBinary());
		assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g: /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					for (String language : Arrays.asList("en", "de")) {
						WebRootResponse response = call(() -> getClient().webroot(PROJECT_NAME,
								new String[] { path1, path2, path3, name + "." + language + ".html" }, new VersioningParameters().draft()));

						assertEquals("Check response language", language, response.getNodeResponse().getLanguage());
					}
				}
			}
		}
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "/News/2015";
		Node newsFolder;
		newsFolder = folder("2015");
		role().revokePermissions(newsFolder, READ_PERM);

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, englishPath, new VersioningParameters().draft());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", newsFolder.getUuid());
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "/News/2015/no-valid-content.html";

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "/News/no-valid-folder/no-valid-content.html";
		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testRead404Page() {
		String notFoundPath = "/error/404";

		Future<WebRootResponse> future = getClient().webroot(PROJECT_NAME, notFoundPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", notFoundPath);
	}

	/**
	 * Test reading the "not found" path /error/404, when this resolves to an existing node. We expect the node to be returned, but the status code still to be
	 * 404
	 */
	@Test
	public void testRead404Node() {
		String notFoundPath = "/error/404";

		NodeCreateRequest createErrorFolder = new NodeCreateRequest();
		createErrorFolder.setSchema(new SchemaReference().setName("folder"));
		createErrorFolder.setParentNodeUuid(project().getBaseNode().getUuid());
		createErrorFolder.getFields().put("name", FieldUtil.createStringField("error"));
		createErrorFolder.setLanguage("en");
		NodeResponse response = call(() -> getClient().createNode(PROJECT_NAME, createErrorFolder));
		String errorNodeUuid = response.getUuid();

		NodeCreateRequest create404Node = new NodeCreateRequest();
		create404Node.setSchema(new SchemaReference().setName("content"));
		create404Node.setParentNodeUuid(errorNodeUuid);
		create404Node.getFields().put("filename", FieldUtil.createStringField("404"));
		create404Node.getFields().put("name", FieldUtil.createStringField("Error Content"));
		create404Node.getFields().put("content", FieldUtil.createStringField("An error happened"));
		create404Node.setLanguage("en");
		call(() -> getClient().createNode(PROJECT_NAME, create404Node));

		Future<WebRootResponse> webrootFuture = getClient().webroot(PROJECT_NAME, notFoundPath, new VersioningParameters().draft());
		latchFor(webrootFuture);
		expectFailureMessage(webrootFuture, NOT_FOUND, null);
	}

	@Test
	public void testReadPublished() {
		String path = "/News/2015";

		try (NoTrx noTx = db.noTrx()) {
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new TakeOfflineParameters().setRecursive(true)));
		}
		// 1. Assert that published path cannot be found
		try (NoTrx noTx = db.noTrx()) {
			call(() -> getClient().webroot(PROJECT_NAME, path, new NodeParameters()), NOT_FOUND, "node_not_found_for_path", path);
		}

		// 2. Publish nodes
		try (NoTrx noTx = db.noTrx()) {
			folder("news").publish(getMockedInternalActionContext(user())).toBlocking().single();
			folder("2015").publish(getMockedInternalActionContext(user())).toBlocking().single();
		}

		// 3. Assert that published path can be found
		try (NoTrx noTx = db.noTrx()) {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, path, new NodeParameters()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("2.0").hasLanguage("en");
		}
	}

	@Test
	public void testReadPublishedDifferentFromDraft() {
		String publishedPath = "/News/2015";
		String draftPath = "/News_draft/2015_draft";

		// 1. Publish nodes
		db.noTrx(() -> {
			folder("news").publish(getMockedInternalActionContext()).toBlocking().single();
			folder("2015").publish(getMockedInternalActionContext()).toBlocking().single();
			return null;
		});

		// 2. Change names
		db.noTrx(() -> {
			updateName(folder("news"), "en", "News_draft");
			updateName(folder("2015"), "en", "2015_draft");
			return null;
		});

		// 3. Assert published path in published
		db.noTrx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, publishedPath, new NodeParameters()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
			return null;
		});

		// 4. Assert published path in draft
		db.noTrx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, publishedPath, new VersioningParameters().draft()), NOT_FOUND, "node_not_found_for_path",
					publishedPath);
			return null;
		});

		// 5. Assert draft path in draft
		db.noTrx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, draftPath, new VersioningParameters().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
			return null;
		});

		// 6. Assert draft path in published
		db.noTrx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, draftPath, new NodeParameters()), NOT_FOUND, "node_not_found_for_path", draftPath);
			return null;
		});
	}

	@Test
	public void testReadForRelease() {
		String newReleaseName = "newrelease";
		String initialPath = "/News/2015";
		String newPath = "/News_new/2015_new";

		// 1. create new release and migrate nodes
		db.noTrx(() -> {
			Release newRelease = project().getReleaseRoot().create(newReleaseName, user());
			nodeMigrationHandler.migrateNodes(newRelease).toBlocking().single();
			return null;
		});

		// 2. update nodes in new release
		db.noTrx(() -> {
			updateName(folder("news"), "en", "News_new");
			updateName(folder("2015"), "en", "2015_new");
			return null;
		});

		// 3. Assert new names in new release
		db.noTrx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, newPath, new VersioningParameters().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
			return null;
		});

		// 4. Assert new names in initial release
		db.noTrx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, newPath,
					new VersioningParameters().draft().setRelease(project().getInitialRelease().getUuid())), NOT_FOUND, "node_not_found_for_path",
					newPath);
			return null;
		});

		// 5. Assert old names in initial release
		db.noTrx(() -> {
			WebRootResponse restNode = call(() -> getClient().webroot(PROJECT_NAME, initialPath,
					new VersioningParameters().draft().setRelease(project().getInitialRelease().getUuid())));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
			return null;
		});

		// 6. Assert old names in new release
		db.noTrx(() -> {
			call(() -> getClient().webroot(PROJECT_NAME, initialPath, new VersioningParameters().draft()), NOT_FOUND, "node_not_found_for_path",
					initialPath);
			return null;
		});
	}

	/**
	 * Update the node name for the latest release
	 * 
	 * @param node
	 *            node
	 * @param language
	 *            language
	 * @param newName
	 *            new name
	 */
	protected void updateName(Node node, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.setVersion(
				new VersionReference(node.getGraphFieldContainer(language).getUuid(), node.getGraphFieldContainer(language).getVersion().toString()));
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), update));
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
		call(() -> getClient().updateNode(PROJECT_NAME, node.getUuid(), update, new VersioningParameters().setRelease(release.getUuid())));
	}
}
