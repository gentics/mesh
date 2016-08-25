package com.gentics.mesh.core.schema;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

public abstract class AbstractChangesVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.eventbusVerticle());
		list.add(meshDagger.adminVerticle());
		list.add(meshDagger.nodeVerticle());
		list.add(meshDagger.schemaVerticle());
		list.add(meshDagger.microschemaVerticle());
		list.add(meshDagger.releaseVerticle());
		return list;
	}

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(meshDagger.nodeMigrationVerticle(), options);
	}

	@After
	public void setopWorkerVerticle() throws Exception {
		meshDagger.nodeMigrationVerticle().stop();
	}

}
