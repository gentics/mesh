package com.gentics.mesh.nav;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.nav.NavigationVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class NavigationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NavigationVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testLoadSimpleNavigation() {
		fail("Not yet implemented");
	}
}
