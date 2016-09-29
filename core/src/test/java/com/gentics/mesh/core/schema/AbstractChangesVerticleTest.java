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
