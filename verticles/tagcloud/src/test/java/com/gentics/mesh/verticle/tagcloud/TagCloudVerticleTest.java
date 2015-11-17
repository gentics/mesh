package com.gentics.mesh.verticle.tagcloud;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class TagCloudVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagCloudVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testTagCloudRead() {
		fail("Not yet implemented");
	}

}
