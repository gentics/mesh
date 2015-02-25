package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.test.AbstractCoreApiVerticleTest;

public class RoleVerticleTest extends AbstractCoreApiVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractCoreApiVerticle getVerticle() {
		return rolesVerticle;
	}

	@Test
	public void testReadRoleByUUID() {
		fail("Not yet implemented");
	}

}
