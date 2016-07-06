package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.MeshAssert;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class SchemaSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(schemaVerticle);
		list.add(eventbusVerticle);
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

	@Test
	public void testSearchSchema() throws InterruptedException, JSONException {
		try (NoTrx noTx = db.noTrx()) {
			fullIndex();
		}

		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleQuery("folder"), new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());

		future = getClient().searchSchemas(getSimpleQuery("blub"), new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(0, response.getData().size());

		future = getClient().searchSchemas(getSimpleTermQuery("name", "folder"), new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		final String newName = "newschema";
		Schema schema = createSchema(newName);
		try (NoTrx noTx = db.noTrx()) {
			MeshAssert.assertElement(boot.schemaContainerRoot(), schema.getUuid(), true);
		}
		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", newName),
				new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		Schema schema = createSchema(schemaName);

		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName),
				new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());

		deleteSchema(schema.getUuid());
		future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName), new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	@Ignore
	public void testDocumentUpdate() throws Exception {
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

		// 1. Create a new schema
		final String schemaName = "newschemaname";
		Schema schema = createSchema(schemaName);

		// 2. Setup latch for migration/schema update
		String newSchemaName = "updatedschemaname";
		updateSchema(schema.getUuid(), newSchemaName);

		// 3. Wait for migration to complete
		failingLatch(latch);

		// 4. Search for the original schema
		Future<SchemaListResponse> future = getClient().searchSchemas(getSimpleTermQuery("name", schemaName),
				new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals("The schema with the old name {" + schemaName + "} was found but it should not have been since we updated it.", 0,
				future.result().getData().size());

		// 5. Search for the updated schema
		future = getClient().searchSchemas(getSimpleTermQuery("name", newSchemaName), new PagingParameters().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		assertEquals("The schema with the updated name was not found.", 1, future.result().getData().size());
	}
}
