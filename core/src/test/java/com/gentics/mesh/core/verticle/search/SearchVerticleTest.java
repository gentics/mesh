package com.gentics.mesh.core.verticle.search;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.project.ProjectSearchVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectSearchVerticle searchVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return searchVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testSearchContent() {
		fail("Not yet implemented");
	}

}
