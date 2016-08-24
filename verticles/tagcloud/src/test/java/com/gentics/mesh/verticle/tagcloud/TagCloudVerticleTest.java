package com.gentics.mesh.verticle.tagcloud;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class TagCloudVerticleTest extends AbstractIsolatedRestVerticleTest {

	private TagCloudVerticle verticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testTagCloudRead() {
		fail("Not yet implemented");
	}

}
