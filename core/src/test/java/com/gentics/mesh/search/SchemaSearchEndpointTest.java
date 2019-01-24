package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;
import com.syncleus.ferma.tx.Tx;

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

		SchemaListResponse response = client()
			.searchSchemas(getSimpleQuery("name", "folder"), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());

		response = client().searchSchemas(getSimpleQuery("name", "blub"), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(0, response.getData().size());

		response = client().searchSchemas(getSimpleTermQuery("name.raw", "folder"), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		final String newName = "newschema";
		SchemaResponse schema = createSchema(newName);
		try (Tx tx = tx()) {
			assertElement(boot().schemaContainerRoot(), schema.getUuid(), true);
		}
		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name.raw", newName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());

		deleteSchema(schema.getUuid());
		response = client().searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(0, response.getData().size());
	}

	@Test
	@Override
	@Ignore
	public void testDocumentUpdate() throws Exception {

		// 1. Create a new schema
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		// 2. Migrate Schema
		String newSchemaName = "updatedschemaname";
		waitForLatestJob(() -> updateSchema(schema.getUuid(), newSchemaName));

		// 3. Search for the original schema
		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals("The schema with the old name {" + schemaName + "} was found but it should not have been since we updated it.", 0,
			response.getData().size());

		// 4. Search for the updated schema
		response = client().searchSchemas(getSimpleTermQuery("name", newSchemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals("The schema with the updated name was not found.", 1, response.getData().size());
	}
}
