package com.gentics.mesh.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.SearchVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SearchVerticle searchVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return searchVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testSearchContent() {
		fail("Not yet implemented");
	}

}
