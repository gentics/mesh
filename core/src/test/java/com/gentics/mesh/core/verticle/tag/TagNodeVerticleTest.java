package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class TagNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadNodesForTag() {
		Future<NodeListResponse> future = getClient().findNodesForTag(DemoDataProvider.PROJECT_NAME, tag("red").getUuid());
		latchFor(future);
		assertSuccess(future);
	}
}
