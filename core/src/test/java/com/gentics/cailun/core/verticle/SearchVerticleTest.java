package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SearchVerticle searchVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return searchVerticle;
	}

	@Test
	public void testSearchContent() {
		fail("Not yet implemented");
	}

}
