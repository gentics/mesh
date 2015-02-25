package com.gentics.cailun.verticle.tagcloud;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;

public class TagCloudVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	private TagCloudVerticle tagCloudVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return tagCloudVerticle;
	}

	@Test
	public void testTagCloudRead() {
		fail("Not yet implemented");
	}

}
