package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
//import com.gentics.mesh.util.DataHelper;

public class ProjectNodeTagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

//	@Autowired
//	private DataHelper helper;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testReadNodeTags() throws Exception {
		Node node = data().getFolder("2015");
		assertNotNull(node);
		System.out.println(node.getUuid());
		assertNotNull(node.getUuid());
		assertNotNull(node.getSchemaContainer());
		
		Future<TagListResponse> future = getClient().findTagsForNode(PROJECT_NAME, node.getUuid());
		latchFor(future);
		assertSuccess(future);
		TagListResponse tagList = future.result();
		assertEquals(4, tagList.getData().size());
		assertEquals(4, tagList.getMetainfo().getTotalCount());
	}

	@Test
	public void testAddTagToNode() throws Exception {

		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		Future<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		assertTrue(test.containsTag(restNode, tag));
		assertTrue(node.getTags().contains(tag));
		// TODO check for properties of the nested tag
	}

	@Test
	public void testAddTagToNoPermNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		info.getRole().revokePermissions(node, UPDATE_PERM);

		Future<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testAddNoPermTagToNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		info.getRole().revokePermissions(tag, READ_PERM);

		Future<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());
		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testRemoveTagFromNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");

		assertTrue(node.getTags().contains(tag));
		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();
		assertFalse(test.containsTag(restNode, tag));
		assertFalse(node.getTags().contains(tag));
		// TODO check for properties of the nested tag

	}

	@Test
	public void testRemoveBogusTagFromNode() throws Exception {
		Node node = data().getFolder("2015");

		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testRemoveTagFromNoPermNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");
		assertTrue(node.getTags().contains(tag));
		info.getRole().revokePermissions(node, UPDATE_PERM);

		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeRequestParameters());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
		assertTrue("The tag should not be removed from the node", node.getTags().contains(tag));
	}

	@Test
	public void testRemoveNoPermTagFromNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");
		assertTrue(node.getTags().contains(tag));
		info.getRole().revokePermissions(tag, READ_PERM);

		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeRequestParameters());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());
		assertTrue("The tag should not have been removed from the node", node.getTags().contains(tag));
	}

}
