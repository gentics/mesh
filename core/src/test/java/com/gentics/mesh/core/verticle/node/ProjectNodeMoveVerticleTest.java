package com.gentics.mesh.core.verticle.node;

import static org.junit.Assert.assertEquals;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.junit.Assert.assertNotEquals;
import io.vertx.core.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectNodeMoveVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		Node sourceNode = folder("news");
		Node targetNode = content("jeep wrangler");

		String oldParentUuid = sourceNode.getParentNode().getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_targetnode_is_no_folder");

		assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode().getUuid());
	}

	@Test
	public void testMoveNodesSame() {
		Node sourceNode = folder("news");
		String oldParentUuid = sourceNode.getParentNode().getUuid();
		assertNotEquals(sourceNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceNode.getUuid(), sourceNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_same_nodes");

		assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode().getUuid());
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		Node sourceNode = folder("news");
		String oldParentUuid = sourceNode.getParentNode().getUuid();
		Node targetNode = folder("2015");
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_not_allowd_to_move_node_into_one_of_its_children");

		assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode().getUuid());
	}

	@Test
	public void testMoveNodeWithPerm() {
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_moved_to", future, sourceNode.getUuid(), targetNode.getUuid());
		assertEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
	}

}
