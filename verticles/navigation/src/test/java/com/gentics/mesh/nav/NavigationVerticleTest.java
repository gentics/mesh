package com.gentics.mesh.nav;

import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.nav.NavigationVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class NavigationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NavigationVerticle navigationVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return navigationVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testLoadSimpleNavigation() {
		fail("Not yet implemented");
	}
}
