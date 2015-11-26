package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class WebRootVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private WebRootVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}
	
	@Test
	public void testReadBinaryNode() {
		fail("not yet tested");
	}

	@Test
	public void testReadFolderByPath() throws Exception {
		Node folder = folder("2015");
		String path = "/News/2015";

		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, path);
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		test.assertMeshNode(folder, restNode);
		// assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
		// restNode.getProperties());
		// assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
		// restNode.getProperties());
	}

	@Test
	public void testReadFolderByPathAndResolveLinks() {
		Node content = content("news_2015");

		content.getGraphFieldContainer(english()).getHtml("content")
				.setHtml("<a href=\"{{mesh.link('" + content.getUuid() + "', 'en')}}\">somelink</a>");
		String path = "/News/2015/News_2015_english_name";

		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setResolveLinks(true).setLanguages("en"));
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		HtmlFieldImpl contentField = restNode.getField("content", HtmlFieldImpl.class);
		assertNotNull(contentField);
		System.out.println(contentField.getHTML());
		test.assertMeshNode(content, restNode);
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/News/2015/News_2015_english_name";
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setLanguages("en", "de"));
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();

		Node node = content("news_2015");
		test.assertMeshNode(node, restNode);
		// assertNotNull(restNode.getProperties());

	}

	@Test
	public void testPathWithSpaces() throws Exception {
		String path = "/News/2015/Special News_2014_english_name";
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, path, new NodeRequestParameter().setLanguages("en", "de"));
		latchFor(future);
		assertSuccess(future);
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String path = "/blub";
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, path);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", "/blub");
	}

	@Test
	public void testReadProjectBaseNode() {
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, "/");
		latchFor(future);
		assertSuccess(future);
		NodeResponse response = future.result();
		assertEquals(project().getBaseNode().getUuid(), response.getUuid());
	}

	@Test
	public void testReadDoubleSlashes() {
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, "//");
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", "//");
	}

	@Test
	public void testReadWithEmptyPath() {
		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, "");
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", "");
	}

	@Test
	public void testReadFolderWithLanguageFallbackInPath() {
		fail("Not yet tested");
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "News/2015";
		Node newsFolder;
		newsFolder = folder("2015");
		role().revokePermissions(newsFolder, READ_PERM);

		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, englishPath);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", newsFolder.getUuid());
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "News/2015/no-valid-content.html";

		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "News/no-valid-folder/no-valid-content.html";

		Future<NodeResponse> future = getClient().webroot(PROJECT_NAME, invalidPath);
		latchFor(future);
		expectException(future, NOT_FOUND, "node_not_found_for_path", invalidPath);
	}

}
