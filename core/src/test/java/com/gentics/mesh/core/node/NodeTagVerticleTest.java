package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeTagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadNodeTags() throws Exception {
		Node node = folder("2015");
		assertNotNull(node);
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
		Node node = folder("2015");
		Tag tag = tag("red");
		assertFalse(node.getTags().contains(tag));

		Future<NodeResponse> future;

		future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		assertSuccess(future);

		node.reload();
		NodeResponse restNode = future.result();
		assertTrue(test.containsTag(restNode, tag));
		assertTrue(node.getTags().contains(tag));

		// TODO check for properties of the nested tag
	}

	@Test
	public void testAddTagToNoPermNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("red");
		assertFalse(node.getTags().contains(tag));
		role().revokePermissions(node, UPDATE_PERM);

		Future<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testAddNoPermTagToNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("red");
		assertFalse(node.getTags().contains(tag));
		role().revokePermissions(tag, READ_PERM);

		Future<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());

		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testRemoveTagFromNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");
		assertTrue(node.getTags().contains(tag));

		Future<NodeResponse> future;
		future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid());
		latchFor(future);
		assertSuccess(future);

		NodeResponse restNode = future.result();
		assertFalse(test.containsTag(restNode, tag));
		node.reload();
		assertFalse(node.getTags().contains(tag));
		// TODO check for properties of the nested tag

	}

	@Test
	public void testRemoveBogusTagFromNode() throws Exception {
		Node node = folder("2015");
		String uuid = node.getUuid();

		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, uuid, "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testRemoveTagFromNoPermNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");
		assertTrue(node.getTags().contains(tag));
		role().revokePermissions(node, UPDATE_PERM);

		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeRequestParameters());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());

		assertTrue("The tag should not be removed from the node", node.getTags().contains(tag));
	}

	@Test
	public void testRemoveNoPermTagFromNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");
		assertTrue(node.getTags().contains(tag));
		role().revokePermissions(tag, READ_PERM);
		Future<NodeResponse> future = getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeRequestParameters());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());

		assertTrue("The tag should not have been removed from the node", node.getTags().contains(tag));
	}

}
