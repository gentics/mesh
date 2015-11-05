package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.node.NodeBreadcrumbResponse;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeBreadcrumbVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testLoadBreadcrumb() throws Exception {
		String nodeUuid = content().getUuid();
		Future<NodeBreadcrumbResponse> future = getClient().loadBreadcrumb(PROJECT_NAME, nodeUuid);
		latchFor(future);
		assertSuccess(future);
	}
}
