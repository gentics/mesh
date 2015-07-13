package com.gentics.mesh.core.verticle.admin;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.AdminVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AdminVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AdminVerticle adminVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return adminVerticle;
	}

	@Test
	@Ignore("Not yet implemented")
	public void testAdminStuff() {
		fail("Not yet implemented");
	}

}
