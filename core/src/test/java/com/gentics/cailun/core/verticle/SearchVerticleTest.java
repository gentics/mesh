package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;

public class SearchVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	public SearchVerticle searchVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return searchVerticle;
	}

	@Test
	public void testSearchContent() {
		fail("Not yet implemented");
	}

}
