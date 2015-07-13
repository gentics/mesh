package com.gentics.mesh.core.verticle.tag;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.project.ProjectTagVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectTagNodeVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectTagVerticle tagVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadNodesForTag() {
		// tags/:uuid/nodes
		fail("Not yet implemented");
	}
}
