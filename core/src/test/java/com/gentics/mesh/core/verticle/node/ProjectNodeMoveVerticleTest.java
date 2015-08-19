package com.gentics.mesh.core.verticle.node;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
public class ProjectNodeMoveVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		String sourceUuid;
		String targetUuid;
		String oldParentUuid;
		try (Trx tx = new Trx(db)) {
			Node sourceNode = folder("news");
			sourceUuid = sourceNode.getUuid();
			Node targetNode = content("jeep wrangler");
			targetUuid = targetNode.getUuid();
			oldParentUuid = sourceNode.getParentNode().getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		}
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceUuid, targetUuid);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_targetnode_is_no_folder");

		try (Trx tx = new Trx(db)) {
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode().getUuid());
		}
	}

	@Test
	public void testMoveNodesSame() {
		String sourceUuid;
		String oldParentUuid;
		try (Trx tx = new Trx(db)) {
			Node sourceNode = folder("news");
			sourceUuid = sourceNode.getUuid();
			oldParentUuid = sourceNode.getParentNode().getUuid();
			assertNotEquals(sourceUuid, sourceNode.getParentNode().getUuid());
		}
		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceUuid, sourceUuid);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_same_nodes");

		try (Trx tx = new Trx(db)) {
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode().getUuid());
		}
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		String sourceUuid;
		String targetUuid;
		String oldParentUuid;
		try (Trx tx = new Trx(db)) {
			Node sourceNode = folder("news");
			sourceUuid = sourceNode.getUuid();
			oldParentUuid = sourceNode.getParentNode().getUuid();
			Node targetNode = folder("2015");
			targetUuid = targetNode.getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		}

		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceUuid, targetUuid);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_not_allowd_to_move_node_into_one_of_its_children");

		try (Trx tx = new Trx(db)) {
			assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode().getUuid());
		}
	}

	@Test
	public void testMoveNodeWithoutPerm() {
		fail("not yet implemented");
	}

	@Test
	public void testMoveNodeWithPerm() {
		String sourceUuid;
		String targetUuid;
		try (Trx tx = new Trx(db)) {
			Node sourceNode = folder("deals");
			sourceUuid = sourceNode.getUuid();
			Node targetNode = folder("2015");
			targetUuid = targetNode.getUuid();
			assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode().getUuid());
		}

		Future<GenericMessageResponse> future = getClient().moveNode(DemoDataProvider.PROJECT_NAME, sourceUuid, targetUuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("node_moved_to", future, sourceUuid, targetUuid);

		try (Trx tx = new Trx(db)) {
			assertEquals("The source node should have been moved and the target uuid should match the parent node uuid of the source node.",
					targetUuid, folder("deals").getParentNode().getUuid());
		}
	}

}
