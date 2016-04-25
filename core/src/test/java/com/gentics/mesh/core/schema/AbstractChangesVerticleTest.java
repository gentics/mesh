package com.gentics.mesh.core.schema;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.DeploymentOptions;

public abstract class AbstractChangesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Autowired
	private AdminVerticle adminVerticle;

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private MicroschemaVerticle microschemaVerticle;

	@Autowired
	private ReleaseVerticle releaseVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(eventbusVerticle);
		list.add(adminVerticle);
		list.add(nodeVerticle);
		list.add(schemaVerticle);
		list.add(microschemaVerticle);
		list.add(releaseVerticle);
		return list;
	}

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(nodeMigrationVerticle, options);
	}

	@After
	public void setopWorkerVerticle() throws Exception {
		nodeMigrationVerticle.stop();
	}

}
