package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return projectVerticle;
	}

	@Test
	public void testReadTagByUUID() {
		fail("Not yet implemented");
	}

}
