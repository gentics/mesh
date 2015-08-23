package com.gentics.mesh.core.verticle.schema;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.MicroschemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class MicroschemaVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private MicroschemaVerticle microschemaVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(microschemaVerticle);
		return list;
	}

	@Ignore("Not yet implemented")
	@Test
	public void testCreateDelete() {

	}

}
