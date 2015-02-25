package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.test.AbstractCoreApiVerticleTest;

public class UserVerticleTest extends AbstractCoreApiVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Override
	public AbstractCoreApiVerticle getVerticle() {
		return userVerticle;
	}

	@Test
	public void testReadUserByUuid() {
		fail("Not yet implemented");
	}

}
