package com.gentics.cailun.core.verticle;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.test.AbstractCoreApiVerticleTest;

public class GroupsVerticleTest extends AbstractCoreApiVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractCoreApiVerticle getVerticle() {
		return groupsVerticle;
	}

	@Test
	public void testReadSimpleGroup() {
		fail("Not yet implemented");
	}

}
