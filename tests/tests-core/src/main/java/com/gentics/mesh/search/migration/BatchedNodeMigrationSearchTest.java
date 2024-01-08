package com.gentics.mesh.search.migration;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = MeshCoreOptionChanger.BATCH_MIGRATION)
public class BatchedNodeMigrationSearchTest extends NodeMigrationSearchTest {

	public BatchedNodeMigrationSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testNodeMigrationBatch() throws TimeoutException {
		final String rootFolderUuid = tx(() -> project().getBaseNode().getUuid());
		final String SCHEMA_NAME = "batchSchema";

		// 1. Create schema
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
		schemaCreateRequest.setName(SCHEMA_NAME);
		schemaCreateRequest.getFields().add(FieldUtil.createStringFieldSchema("name").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaCreateRequest));

		// 2. Assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		waitForSearchIdleEvent();

		// 3. Create nodes
		for (int i = 0; i < 100; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(rootFolderUuid);
			nodeCreateRequest.setSchemaName(SCHEMA_NAME);
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("value"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			assertEquals("Schema version does not match expected " + schemaResponse.getVersion(), 
					schemaResponse.getVersion(), response.getSchema().getVersion());
		}

		waitForSearchIdleEvent();

		// Before update
		assertEquals(100, call(() -> client().searchNodes(PROJECT_NAME, queryName("value"))).getMetainfo().getTotalCount());

		// 4. Update schema 
		SchemaUpdateRequest schemaUpdateRequest = schemaResponse.toUpdateRequest();
		schemaUpdateRequest.getFields().add(FieldUtil.createStringFieldSchema("another"));
		grantAdmin();

		waitForJobs(() -> call(() -> client().updateSchema(schemaResponse.getUuid(), schemaUpdateRequest)), JobStatus.COMPLETED, 1, 120);
		waitForSearchIdleEvent();
		// we need to check the index mapping, because when updating the schema, it might happen that the new ES index is created "on the fly" (during the node migration)
		// and will therefore have the wrong mapping
		checkIndexMapping(10_000);

		// After update
		NodeListResponse searchResponse = call(() -> client().searchNodes(PROJECT_NAME, queryName("value")));
		assertEquals(100, searchResponse.getMetainfo().getTotalCount());
		for (NodeResponse response : searchResponse.getData()) {
			assertNotEquals("Schema version should not match expected " + schemaResponse.getVersion(), 
					schemaResponse.getVersion(), response.getSchema().getVersion());
		}
	}
}
