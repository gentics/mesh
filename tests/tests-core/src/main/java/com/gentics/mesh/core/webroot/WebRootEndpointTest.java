package com.gentics.mesh.core.webroot;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.parameter.LinkType.MEDIUM;
import static com.gentics.mesh.parameter.LinkType.SHORT;
import static com.gentics.mesh.test.AWSTestMode.MINIO;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.CoreTestUtils;
import com.gentics.mesh.util.URIUtils;

@MeshTestSetting(awsContainer = MINIO, testSize = FULL, startServer = true)
public class WebRootEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadBinaryNode() throws IOException {
		HibNode node = content("news_2015");
		String nodeUuid = tx(() -> node.getUuid());

		try (Tx tx = tx()) {
			// 1. Transform the node into a binary content
			prepareSchema(node, "image/*", "binary");
			tx.success();
		}

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";

		// 2. Update the binary data
		call(() -> uploadRandomData(content("news_2015"), "en", "binary", binaryLen, contentType, fileName));

		// 3. Try to resolve the path
		String path = "/News/2015/somefile.dat";
		MeshWebrootResponse response = call(() -> client().webroot(PROJECT_NAME, path,
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
		MeshBinaryResponse downloadResponse = response.getBinaryResponse();
		assertEquals("Webroot response node uuid header value did not match", nodeUuid, response.getNodeUuid());
		assertTrue(response.isBinary());
		assertNotNull(downloadResponse);
	}

	@Test
	public void testReadS3BinaryNode() throws IOException {
		HibNode node = content("news_2015");
		String nodeUuid = tx(() -> node.getUuid());

		try (Tx tx = tx()) {
			prepareTypedSchema(node, new S3BinaryFieldSchemaImpl().setAllowedMimeTypes("image/*").setName("s3").setLabel("Binary content"), true);
			tx.success();
		}

		S3BinaryUploadRequest request = new S3BinaryUploadRequest().setFilename("test.jpg").setLanguage("en")
				.setVersion("1.0");

		// 2. Update the binary data
		call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, nodeUuid, "s3", request));
		// uploading
		String s3Bucket = getTestContext().getOptions().getS3Options().getBucket();
		File tempFile = createTempFile();
		s3BinaryStorage().createBucket(s3Bucket)
				.flatMap(unused -> s3BinaryStorage().uploadFile(s3Bucket, nodeUuid + "/s3/en", tempFile, false))
				.blockingGet();

		// 3. Try to resolve the path
		String path = "/News/2015/test.jpg";
		MeshWebrootResponse response = call(() -> client().webroot(PROJECT_NAME, path,
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
		assertNotNull(response);
		assertTrue(response.isBinary());

		MeshBinaryResponse downloadResponse = response.getBinaryResponse();
		assertNotNull(downloadResponse);
		assertEquals("image/jpeg", downloadResponse.getContentType());

		BufferedImage buf;
		try (InputStream is = downloadResponse.getStream()) {
			byte[] downloadBytes = IOUtils.toByteArray(is);
			InputStream in = new ByteArrayInputStream(downloadBytes);
			buf = ImageIO.read(in);

			assertEquals(1160, buf.getWidth());
			assertEquals(1376, buf.getHeight());
		} catch (IOException ioException) {
			fail(ioException.getLocalizedMessage());
		}
	}

	@Test
	public void testReadFolderByPath() throws Exception {
		HibNode node = folder("2015");
		String nodeUuid = tx(() -> node.getUuid());
		String path = "/News/2015";

		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft()));
		assertThat(response.getNodeResponse()).is(node).hasLanguage("en");
		assertFalse(response.isBinary());
		assertEquals("Webroot response node uuid header value did not match", nodeUuid, response.getNodeUuid());
	}

	@Test
	public void testInvalidWebrootPath() {
		HibNode node = folder("2015");
		String path = "/News/2015";

		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft()));
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft()));

		// Modify the cache entry by adding another bogus segment. The validation should
		// pick up the inconsistency and invalidate the whole path entry
		tx(tx -> {
			HibNodeFieldContainer bogusContainer = CoreTestUtils.createContainer();
			HibStringField bogusField = bogusContainer.createString("name");
			Path entry = mesh().pathCache().getPath(project(), initialBranch(), ContainerType.DRAFT, path);
			entry.addSegment(new PathSegmentImpl(bogusContainer, bogusField, "en", "bogus"));
			tx.rollback();
		});

		// Now ensure that the bogus cache entry is ignored and regular response is
		// returned
		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft()));
		assertThat(response.getNodeResponse()).is(node).hasLanguage("en");
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		HibNode content = content("news_2015");

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			NodeResponse response = call(() -> client().findNodeByUuid(projectName(), content.getUuid()));
			NodeUpdateRequest request = response.toRequest();
			request.getFields()
					.put("content", new HtmlFieldImpl().setHTML("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>"));
			call(() -> client().updateNode(projectName(), content.getUuid(), request));
			tx.success();
		}

		try (Tx tx = tx()) {
			String path = "/News/2015/News_2015.en.html";
			MeshWebrootResponse restNode = call(
					() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
							new NodeParametersImpl().setResolveLinks(LinkType.FULL).setLanguages("en")));
			HtmlFieldImpl contentField = restNode.getNodeResponse().getFields().getHtmlField("content");
			assertNotNull(contentField);
			assertEquals("Check rendered content",
					"<a href=\"" + CURRENT_API_BASE_PATH + "/dummy/webroot/News/2015/News_2015.en.html\">somelink</a>",
					contentField.getHTML());
			assertThat(restNode.getNodeResponse()).is(content).hasLanguage("en");
		}

	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015.en.html";

		MeshWebrootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path,
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages("en", "de")));

		try (Tx tx = tx()) {
			HibNode node = content("news_2015");
			assertThat(restNode.getNodeResponse()).is(node).hasLanguage("en");
		}
	}

	@Test
	public void testReadContentWithNodeRefByPath() throws Exception {
		// Update content schema and add node field
		String json = tx(() -> schemaContainer("folder").getLatestVersion().getJson());
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		waitForJob(() -> {
			SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
			schema.getFields().add(FieldUtil.createNodeFieldSchema("nodeRef"));
			call(() -> client().updateSchema(uuid, schema));
		});

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			NodeDao nodeDao = tx.nodeDao();
			RoleDao roleDao = tx.roleDao();
			HibNode parentNode = folder("2015");
			// Create content which is only german
			HibSchema contentSchema = schemaContainer("content");
			HibNode node = nodeDao.create(parentNode, user(), contentSchema.getLatestVersion(), project());

			// Grant permissions to the node otherwise it will not be able to be loaded
			roleDao.grantPermissions(role(), node, InternalPermission.values());
			HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, german(), project().getLatestBranch(), user());
			englishContainer.createString("teaser").setString("german teaser");
			englishContainer.createString("title").setString("german title");
			//englishContainer.createString("displayName").setString("german displayName");
			englishContainer.createString("slug").setString("test.de.html");

			// Add node reference to node 2015
			HibNodeFieldContainer original = contentDao.getLatestDraftFieldContainer(parentNode, english());
			HibNodeFieldContainer container = contentDao.createFieldContainer(parentNode, english(), project().getLatestBranch(), user(), original, true);
			container.createNode("nodeRef", node);
			contentDao.updateWebrootPathInfo(container, project().getLatestBranch().getUuid(), "");
			tx.success();
		}
		String path = "/News/2015";
		MeshWebrootResponse restNode = call(
				() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setResolveLinks(MEDIUM).setLanguages("en", "de")));
		assertEquals("The node reference did not point to the german node.", "/dummy/News/2015/test.de.html",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015",
				restNode.getNodeResponse().getFields().getStringField("name").getString());

		// Again with no german fallback option (only english)
		restNode = call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setResolveLinks(LinkType.MEDIUM).setLanguages("en")));
		assertEquals("The node reference did not point to the 404 path.", "/dummy/error/404",
				restNode.getNodeResponse().getFields().getNodeField("nodeRef").getPath());
		assertEquals("The name of the node did not match", "2015",
				restNode.getNodeResponse().getFields().getStringField("name").getString());
	}

	@Test
	public void testReadMultithreaded() {
		int nJobs = 200;
		String path = "/News/2015/News_2015.en.html";

		awaitConcurrentRequests(nJobs, i -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
				new NodeParametersImpl().setLanguages("en", "de")));
	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String[] path = new String[] { "News", "2015", "Special News_2014.en.html" };
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
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
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
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
		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
						new NodeParametersImpl().setLanguages("en", "de").setResolveLinks(SHORT)));
		assertEquals(uuid, response.getNodeResponse().getUuid());
		assertEquals("/News/" + URIUtils.encodeSegment(newName), response.getNodeResponse().getPath());
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
		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().draft(),
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
		MeshWebrootResponse response = client().webroot(PROJECT_NAME, "").blockingGet();
		assertEquals(project().getBaseNode().getUuid(), response.getNodeResponse().getUuid());
	}

	@Test
	public void testReadProjectBaseNode() {
		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, "/", new VersioningParametersImpl().draft()));
		assertFalse(response.isBinary());
		try (Tx tx = tx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(),
					response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadDoubleSlashes() {
		MeshWebrootResponse response = call(
				() -> client().webroot(PROJECT_NAME, "//", new VersioningParametersImpl().draft()));
		assertFalse(response.isBinary());
		try (Tx tx = tx()) {
			assertEquals("We expected the project basenode.", project().getBaseNode().getUuid(),
					response.getNodeResponse().getUuid());
		}
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		// Test requesting a path that contains of mixed language segments: e.g:
		// /Fahrzeuge/Cars/auto.html
		String name = "New_in_March_2014";
		for (String path1 : Arrays.asList("News", "Neuigkeiten")) {
			for (String path2 : Arrays.asList("2014")) {
				for (String path3 : Arrays.asList("March", "März")) {
					for (String language : Arrays.asList("en", "de")) {
						MeshWebrootResponse response = call(() -> client().webroot(PROJECT_NAME,
								new String[] { path1, path2, path3, name + "." + language + ".html" },
								new VersioningParametersImpl().draft()));

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
			RoleDao roleDao = tx.roleDao();
			HibNode newsFolder = folder("2015");
			uuid = newsFolder.getUuid();
			roleDao.revokePermissions(role(), newsFolder, READ_PERM);
			roleDao.revokePermissions(role(), newsFolder, READ_PUBLISHED_PERM);
			tx.success();
		}

		call(() -> client().webroot(PROJECT_NAME, englishPath, new VersioningParametersImpl().draft()), FORBIDDEN,
				"error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
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

			call(() -> client().webroot(PROJECT_NAME, notFoundPath, new VersioningParametersImpl().draft()), NOT_FOUND);
		}
	}

	@Test
	public void testWebrootCacheControlPrivateNode() {
		String path = "/News/2015";

		MeshResponse<MeshWebrootResponse> response = client()
				.webroot(PROJECT_NAME, path, new VersioningParametersImpl().published()).getResponse().blockingGet();
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

		MeshResponse<MeshWebrootResponse> response = client()
				.webroot(PROJECT_NAME, path, new VersioningParametersImpl().published()).getResponse().blockingGet();
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

		MeshResponse<MeshWebrootResponse> response = client()
				.webroot(PROJECT_NAME, path, new VersioningParametersImpl().published()).getResponse().blockingGet();
		String cacheControl = response.getHeader("Cache-Control").get();
		assertEquals("public", cacheControl);

		// Read again - this time the draft. The anonymous role is not allowed to read
		// this
		response = client().webroot(PROJECT_NAME, path).getResponse().blockingGet();
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
		call(() -> client().webroot(PROJECT_NAME, path, new VersioningParametersImpl().published()), NOT_FOUND,
				"node_not_found_for_path", path);

		// 2. Publish nodes
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			nodeDao.publish(folder("news"), mockActionContext());
			nodeDao.publish(folder("2015"), mockActionContext());
			tx.success();
		}

		// 3. Assert that published path can be found
		try (Tx tx = tx()) {
			MeshWebrootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new NodeParametersImpl()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("2.0").hasLanguage("en");
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
			MeshWebrootResponse restNode = call(() -> client().webroot(PROJECT_NAME, path, new NodeParametersImpl(),
					new VersioningParametersImpl().published()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
		}
	}

	@Test
	public void testReadPublishedDifferentFromDraft() {
		String publishedPath = "/News/2015";
		String draftPath = "/News_draft/2015_draft";

		// 1. Publish nodes
		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			nodeDao.publish(folder("news"), mockActionContext());
			nodeDao.publish(folder("2015"), mockActionContext());
		});

		// 2. Change names
		String newsUuid = tx(() -> folder("news").getUuid());
		updateSlug(newsUuid, "en", "News_draft", initialBranchUuid());
		String folder2015Uuid = tx(() -> folder("2015").getUuid());
		updateSlug(folder2015Uuid, "en", "2015_draft", initialBranchUuid());

		// 3. Assert published path in published
		tx(() -> {
			MeshWebrootResponse restNode = call(
					() -> client().webroot(PROJECT_NAME, publishedPath, new VersioningParametersImpl().published()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.0").hasLanguage("en");
		});

		// 4. Assert published path in draft
		tx(tx -> {
			call(() -> client().webroot(PROJECT_NAME, publishedPath, new VersioningParametersImpl().draft()), NOT_FOUND,
					"node_not_found_for_path", publishedPath);
		});

		// 5. Assert draft path in draft
		tx(() -> {
			MeshWebrootResponse restNode = call(
					() -> client().webroot(PROJECT_NAME, draftPath, new VersioningParametersImpl().draft()));
			assertThat(restNode.getNodeResponse()).is(folder("2015")).hasVersion("1.1").hasLanguage("en");
		});

		// 6. Assert draft path in published
		tx(() -> {
			call(() -> client().webroot(PROJECT_NAME, draftPath, new VersioningParametersImpl().published()), NOT_FOUND,
					"node_not_found_for_path", draftPath);
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
		MeshWebrootResponse restNode2 = call(() -> client().webroot(PROJECT_NAME, initialPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		assertThat(restNode2.getNodeResponse()).hasUuid(folder2015Uuid).hasVersion("1.0").hasLanguage("en");

		// Assert name in new branch after migration
		MeshWebrootResponse restNode3 = call(() -> client().webroot(PROJECT_NAME, initialPath,
				new VersioningParametersImpl().draft().setBranch(newBranchName)));
		assertThat(restNode3.getNodeResponse()).hasUuid(folder2015Uuid).hasVersion("1.0").hasLanguage("en");

		// 2. update nodes in new branch
		updateSlug(newsUuid, "en", "News_new", newBranchName);
		updateSlug(folder2015Uuid, "en", "2015_new", newBranchName);

		// NodeResponse node = call(() -> client().findNodeByUuid(PROJECT_NAME,
		// folder2015Uuid,
		// new VersioningParametersImpl().setBranch(initialBranchUuid()), new
		// NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		//
		// NodeResponse node2 = call(() -> client().findNodeByUuid(PROJECT_NAME,
		// folder2015Uuid, new VersioningParametersImpl().setBranch(newBranchName),
		// new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));

		// 3. Assert new name in new branch
		MeshWebrootResponse restNode = call(() -> client().webroot(PROJECT_NAME, newPath,
				new VersioningParametersImpl().draft().setBranch(newBranchName)));
		assertThat(restNode.getNodeResponse()).hasUuid(folder2015Uuid).hasVersion("1.1").hasLanguage("en");

		// 4. Assert new name in initial branch
		call(() -> client().webroot(PROJECT_NAME, newPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())), NOT_FOUND,
				"node_not_found_for_path", newPath);

		// 5. Assert old names in initial branch
		MeshWebrootResponse restNode4 = call(() -> client().webroot(PROJECT_NAME, initialPath,
				new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		assertThat(restNode4.getNodeResponse()).hasUuid(folder2015Uuid).hasVersion("1.0").hasLanguage("en");

		// 6. Assert old names in new branch
		call(() -> client().webroot(PROJECT_NAME, initialPath, new VersioningParametersImpl().draft()), NOT_FOUND,
				"node_not_found_for_path", initialPath);
	}

	/**
	 * Update the node slug field for the latest branch.
	 * 
	 * @param uuid     node
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
	protected void updateName(HibNode node, HibBranch branch, String language, String newName) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(language);
		update.getFields().put("name", FieldUtil.createStringField(newName));
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), update,
				new VersioningParametersImpl().setBranch(branch.getUuid())));
	}
}
