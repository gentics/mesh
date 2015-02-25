package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.test.AbstractCoreApiVerticleTest;

public class ProjectVerticleTest extends AbstractCoreApiVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public AbstractCoreApiVerticle getVerticle() {
		return projectVerticle;
	}

	@Test
	public void testReadTagByUUID() {
		fail("Not yet implemented");
	}

}
