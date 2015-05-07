package com.gentics.mesh.core.verticle;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AdminVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AdminVerticle adminVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return adminVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testAdminStuff() {
		fail("Not yet implemented");
	}

}
