package com.gentics.mesh.core.tag;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.verticle.tag.TagVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class TagNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadNodesForTag() {
		Future<NodeListResponse> future = getClient().findNodesForTag(PROJECT_NAME, tag("red").getUuid());
		latchFor(future);
		assertSuccess(future);
	}
}
