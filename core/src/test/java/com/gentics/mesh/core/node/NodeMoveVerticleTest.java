package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeMoveVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		Node sourceNode = folder("news");
		Node targetNode = content("concorde");
		String oldParentUuid = sourceNode.getParentNode().getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode().getUuid());
	}

	@Test
	public void testMoveNodesSame() {
		Node sourceNode = folder("news");
		String oldParentUuid = sourceNode.getParentNode().getUuid();
		assertNotEquals(sourceNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), sourceNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_same_nodes");
		assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode().getUuid());
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		Node sourceNode = folder("news");
		Node targetNode = folder("2015");
		String oldParentUuid = sourceNode.getParentNode().getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());

		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_not_allowd_to_move_node_into_one_of_its_children");

		assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode().getUuid());
	}

	@Test
	public void testMoveNodeWithoutPerm() {
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		role().revokePermissions(sourceNode, GraphPermission.UPDATE_PERM);

		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", sourceNode.getUuid());
		assertNotEquals("The source node should not have been moved.", targetNode.getUuid(), folder("deals").getParentNode().getUuid());
	}

	@Test
	public void testMoveNodeWithPerm() {

		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		String oldSourceParentId = sourceNode.getParentNode().getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_moved_to", future, sourceNode.getUuid(), targetNode.getUuid());

		sourceNode.reload();
		try (Trx tx = db.trx()) {
			assertNotEquals("The source node parent uuid should have been updated.", oldSourceParentId, sourceNode.getParentNode().getUuid());
			assertEquals("The source node should have been moved and the target uuid should match the parent node uuid of the source node.",
					targetNode.getUuid(), sourceNode.getParentNode().getUuid());
			assertEquals(2, searchProvider.getStoreEvents().size());
		}
		// TODO assert entries
	}

}
