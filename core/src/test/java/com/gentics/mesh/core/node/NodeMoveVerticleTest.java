package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class NodeMoveVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testMoveNodeIntoNonFolderNode() {
		String releaseUuid = project().getLatestRelease().getUuid();
		Node sourceNode = folder("news");
		Node targetNode = content("concorde");
		String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(releaseUuid).getUuid());
	}

	@Test
	public void testMoveNodesSame() {
		String releaseUuid = project().getLatestRelease().getUuid();
		Node sourceNode = folder("news");
		String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
		assertNotEquals(sourceNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), sourceNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_same_nodes");
		assertEquals("The node should not have been moved but it was.", oldParentUuid, folder("news").getParentNode(releaseUuid).getUuid());
	}

	@Test
	public void testMoveNodeIntoChildNode() {
		String releaseUuid = project().getLatestRelease().getUuid();
		Node sourceNode = folder("news");
		Node targetNode = folder("2015");
		String oldParentUuid = sourceNode.getParentNode(releaseUuid).getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());

		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_move_error_not_allowed_to_move_node_into_one_of_its_children");

		assertEquals("The node should not have been moved but it was.", oldParentUuid, sourceNode.getParentNode(releaseUuid).getUuid());
	}

	@Test
	public void testMoveNodeWithoutPerm() {
		String releaseUuid = project().getLatestRelease().getUuid();
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
		role().revokePermissions(sourceNode, GraphPermission.UPDATE_PERM);

		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", sourceNode.getUuid());
		assertNotEquals("The source node should not have been moved.", targetNode.getUuid(), folder("deals").getParentNode(releaseUuid).getUuid());
	}

	@Test
	public void testMoveNodeWithPerm() {
		String releaseUuid = project().getLatestRelease().getUuid();
		Node sourceNode = folder("deals");
		Node targetNode = folder("2015");
		String oldSourceParentId = sourceNode.getParentNode(releaseUuid).getUuid();
		assertNotEquals(targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
		Future<GenericMessageResponse> future = getClient().moveNode(PROJECT_NAME, sourceNode.getUuid(), targetNode.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "node_moved_to", sourceNode.getUuid(), targetNode.getUuid());

		sourceNode.reload();
		try (Trx tx = db.trx()) {
			assertNotEquals("The source node parent uuid should have been updated.", oldSourceParentId, sourceNode.getParentNode(releaseUuid).getUuid());
			assertEquals("The source node should have been moved and the target uuid should match the parent node uuid of the source node.",
					targetNode.getUuid(), sourceNode.getParentNode(releaseUuid).getUuid());
			assertEquals(2, searchProvider.getStoreEvents().size());
		}
		// TODO assert entries
	}

	@Test
	public void testMoveInRelease() {
		Project project = project();
		Node movedNode = folder("deals");
		Node targetNode = folder("2015");

		// get original parent uuid
		String oldParentUuid = call(
				() -> getClient().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParameters().draft()))
						.getParentNode().getUuid();

		Release initialRelease = project.getInitialRelease();
		Release newRelease = project.getReleaseRoot().create("newrelease", user());

		NodeResponse migrated = migrateNode(PROJECT_NAME, movedNode.getUuid(), initialRelease.getName(), newRelease.getName());
		assertThat(migrated.getParentNode()).as("Migrated node parent").isNotNull();
		assertThat(migrated.getParentNode().getUuid()).as("Migrated node parent").isEqualTo(oldParentUuid);

		// move in initial release
		call(() -> getClient().moveNode(PROJECT_NAME, movedNode.getUuid(), targetNode.getUuid(),
				new VersioningParameters().setRelease(initialRelease.getName())));

		// old parent for new release
		assertThat(call(
				() -> getClient().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParameters().draft()))
						.getParentNode().getUuid()).as("Parent Uuid in new release").isEqualTo(oldParentUuid);

		// new parent for initial release
		assertThat(call(
				() -> getClient().findNodeByUuid(PROJECT_NAME, movedNode.getUuid(), new VersioningParameters().setRelease(initialRelease.getName()).draft()))
						.getParentNode().getUuid()).as("Parent Uuid in initial release").isEqualTo(targetNode.getUuid());
	}
}
