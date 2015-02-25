package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;

public class TagVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	private TagVerticle tagVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadTagByUUID() {
		fail("Not yet implemented");
	}

}
