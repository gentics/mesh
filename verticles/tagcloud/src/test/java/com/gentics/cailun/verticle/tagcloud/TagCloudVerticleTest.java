package com.gentics.cailun.verticle.tagcloud;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class TagCloudVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagCloudVerticle tagCloudVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagCloudVerticle;
	}

	@Test
	public void testTagCloudRead() {
		fail("Not yet implemented");
	}

}
