package com.gentics.mesh.core.verticle.tag;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.TagVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class TagNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagVerticle tagVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadNodesForTag() {
		// tags/:uuid/nodes
		fail("Not yet implemented");
	}
}
