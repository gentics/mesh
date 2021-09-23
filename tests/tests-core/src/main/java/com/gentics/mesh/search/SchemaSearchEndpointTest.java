package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getUuidQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

import io.vertx.core.DeploymentOptions;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public SchemaSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

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
	public void testEmptySchema() throws Exception {
		final String SCHEMA_NAME = "TestSchema";
		final String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
		grantAdmin();
		try (Tx tx = tx()) {
			recreateIndices();
		}

		// 1. Create empty schema
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		SchemaResponse response = call(() -> client().createSchema(request));
		String uuid = response.getUuid();
		call(() -> client().assignSchemaToProject(projectName(), uuid));

		// 2. Create a test node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName(SCHEMA_NAME);
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		NodeResponse nodeResponse = call(() -> client().createNode(projectName(), nodeCreateRequest));

		// 3. Update the schema and add a field
		waitForJob(() -> {
			SchemaUpdateRequest updateRequest = response.toUpdateRequest();
			updateRequest.setDescription("Some Description");
			updateRequest.setFields(Arrays.asList(FieldUtil.createStringFieldSchema("test")));
			call(() -> client().updateSchema(uuid, updateRequest));
		});

		// Wait and reload the node
		waitForSearchIdleEvent();
		String version = call(() -> client().findNodeByUuid(projectName(), nodeResponse.getUuid())).getVersion();
		System.out.println("version:" + version);
		assertEquals("The version should be bumped via the migration.", "0.2", version);

		// Search for the node
		NodeListResponse searchResponse = client().searchNodes(getUuidQuery(nodeResponse.getUuid())).blockingGet();
		assertEquals(1, searchResponse.getData().size());
		assertEquals(nodeResponse.getUuid(), searchResponse.getData().get(0).getUuid());
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

		response = client().searchSchemas(getSimpleTermQuery("name.raw", "folder"), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		final String newName = "newschema";
		SchemaResponse schema = createSchema(newName);
		try (Tx tx = tx()) {
			assertNotNull(boot().schemaDao().findByUuid(schema.getUuid()));
		}
		waitForSearchIdleEvent();

		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name.raw", newName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);

		waitForSearchIdleEvent();
		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals(1, response.getData().size());

		deleteSchema(schema.getUuid());
		response = client().searchSchemas(getSimpleTermQuery("name.raw", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals(0, response.getData().size());
	}

	@Test
	@Override
	@Ignore
	public void testDocumentUpdate() throws Exception {

		// 1. Create a new schema
		final String schemaName = "newschemaname";
		SchemaResponse schema = createSchema(schemaName);
		waitForSearchIdleEvent();

		// 2. Migrate Schema
		String newSchemaName = "updatedschemaname";
		waitForLatestJob(() -> updateSchema(schema.getUuid(), newSchemaName));
		waitForSearchIdleEvent();

		// 3. Search for the original schema
		SchemaListResponse response = client()
			.searchSchemas(getSimpleTermQuery("name", schemaName), new PagingParametersImpl().setPage(1).setPerPage(2L)).blockingGet();
		assertEquals("The schema with the old name {" + schemaName + "} was found but it should not have been since we updated it.", 0,
			response.getData().size());

		// 4. Search for the updated schema
		response = client().searchSchemas(getSimpleTermQuery("name", newSchemaName), new PagingParametersImpl().setPage(1).setPerPage(2L))
			.blockingGet();
		assertEquals("The schema with the updated name was not found.", 1, response.getData().size());
	}
}
