package com.gentics.mesh.verticle.tagcloud;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.verticle.tagcloud.TagCloudVerticle;

public class TagCloudVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagCloudVerticle tagCloudVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return tagCloudVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testTagCloudRead() {
		fail("Not yet implemented");
	}

}
