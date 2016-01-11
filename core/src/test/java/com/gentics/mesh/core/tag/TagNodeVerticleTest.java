package com.gentics.mesh.core.tag;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class TagNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagFamilyVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadNodesForTag() {
		Future<NodeListResponse> future = getClient().findNodesForTag(PROJECT_NAME, tagFamily("colors").getUuid(), tag("red").getUuid());
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());
	}
}
