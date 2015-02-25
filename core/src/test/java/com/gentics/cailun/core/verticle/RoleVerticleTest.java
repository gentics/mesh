package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class RoleVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return rolesVerticle;
	}

	@Test
	public void testReadRoleByUUID() {
		fail("Not yet implemented");
	}

}
