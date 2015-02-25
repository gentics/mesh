package com.gentics.cailun.nav;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;

public class NavigationVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	private NavigationVerticle navigationVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return navigationVerticle;
	}

	@Test
	public void testLoadSimpleNavigation() {
		fail("Not yet implemented");
	}
}
