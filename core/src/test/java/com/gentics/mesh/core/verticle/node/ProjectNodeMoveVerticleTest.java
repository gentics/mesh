package com.gentics.mesh.core.verticle.node;

import static org.junit.Assert.assertEquals;
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
