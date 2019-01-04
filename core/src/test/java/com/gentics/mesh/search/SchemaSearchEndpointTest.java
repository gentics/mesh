package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.madl.tx.Tx;

import io.vertx.core.DeploymentOptions;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class SchemaSearchEndpointTest extends AbstractMeshTest implements BasicSearchCrudTestcases {

	@Before
	public void setupWorkerVerticle() throws Exception {
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx().deployVerticle(meshDagger().jobWorkerVerticle(), options);
	}

	@After
	public void removeWorkerVerticle() throws Exception {
		meshDagger().jobWorkerVerticle().stop();
	}

	@Test
	public void testSearchSchema() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		MeshResponse<SchemaListResponse> future = client()
				.searchSchemas(getSimpleQuery("name", "folder"), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());

		future = client().searchSchemas(getSimpleQuery("name", "blub"), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(0, response.getData().size());

		future = client().searchSchemas(getSimpleTermQuery("name.raw", "folder"), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		final String newName = "newschema";
		SchemaResponse schema = createSchema(newName);
		try (Tx tx = tx()) {
			MeshAssert.assertElement(boot().schemaContainerRoot(), schema.getUuid(), true);
		}
		MeshResponse<SchemaListResponse> future = client()
				.searchSchemas(getSimpleTermQuery("name.raw", newName), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse response = future.result();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		MeshResponse<SchemaListResponse> future = client()
				.searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());

		deleteSchema(schema.getUuid());
		future = client().searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	@Ignore
	public void testDocumentUpdate() throws Exception {
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());

		// 1. Create a new schema
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		// 2. Setup latch for migration/schema update
		String newSchemaName = "updatedschemaname";
		updateSchema(schema.getUuid(), newSchemaName);

		// 3. Wait for migration to complete
		failingLatch(latch);

		// 4. Search for the original schema
		MeshResponse<SchemaListResponse> future = client()
				.searchSchemas(getSimpleTermQuery("name", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals("The schema with the old name {" + schemaName + "} was found but it should not have been since we updated it.", 0,
				future.result().getData().size());

		// 5. Search for the updated schema
		future = client().searchSchemas(getSimpleTermQuery("name", newSchemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals("The schema with the updated name was not found.", 1, future.result().getData().size());
	}
}
