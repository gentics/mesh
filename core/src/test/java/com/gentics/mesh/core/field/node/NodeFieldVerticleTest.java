package com.gentics.mesh.core.field.node;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class NodeFieldVerticleTest extends AbstractRestVerticleTest {
	@Autowired
	private ProjectNodeVerticle verticle;

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		super.setupVerticleTest();
	}

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}
	
	@Test
	public void testNodeFieldVerticleViaRest() {
		
	}
}
