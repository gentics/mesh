package com.gentics.mesh.core.verticle.schema;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.MicroschemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class MicroschemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private MicroschemaVerticle microschemaVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return microschemaVerticle;
	}

	@Ignore("Not yet implemented")
	@Test
	public void testCreateDelete() {

	}

}
