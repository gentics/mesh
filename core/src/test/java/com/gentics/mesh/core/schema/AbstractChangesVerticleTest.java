package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.verticle.eventbus.EventbusAddress.MESH_MIGRATION;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractChangesVerticleTest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractChangesVerticleTest.class);

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

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(eventbusVerticle);
		list.add(adminVerticle);
		list.add(nodeVerticle);
		list.add(microschemaVerticle);
		list.add(schemaVerticle);
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

	/**
	 * Construct a latch which will release when the migration has finished.
	 * 
	 * @return
	 */
	protected CountDownLatch latchForMigrationCompleted() {
		// Construct latch in order to wait until the migration completed event was received 
		CountDownLatch latch = new CountDownLatch(1);
		getClient().eventbus(ws -> {
			// Register to migration events
			JsonObject msg = new JsonObject().put("type", "register").put("address", MESH_MIGRATION.toString());
			ws.writeFinalTextFrame(msg.encode());

			// Handle migration events
			ws.handler(buff -> {
				String str = buff.toString();
				JsonObject received = new JsonObject(str);
				JsonObject rec = received.getJsonObject("body");
				log.debug("Migration event:" + rec.getString("type"));
				if ("completed".equalsIgnoreCase(rec.getString("type"))) {
					latch.countDown();
				}
			});

		});
		return latch;
	}

}
