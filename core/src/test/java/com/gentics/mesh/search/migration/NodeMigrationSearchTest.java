package com.gentics.mesh.search.migration;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.category.FailingTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeMigrationSearchTest extends AbstractNodeSearchEndpointTest {

	public NodeMigrationSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	public String queryName(String value) {
		return getSimpleTermQuery("fields.name.raw", value);
	}

	@Test
	public void testNodeMigrationConflict() {
		final String rootFolderUuid = tx(() -> project().getBaseNode().getUuid());
		final String SCHEMA_NAME = "testSchema";

		// 1. Create schema
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
		schemaCreateRequest.setName(SCHEMA_NAME);
		schemaCreateRequest.getFields().add(FieldUtil.createStringFieldSchema("name").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse schemaResponse = call(() -> client().createSchema(schemaCreateRequest));

		// 2. Assign schema to project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

		// 3. Create nodes
		for (int i = 0; i < 5; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(rootFolderUuid);
			nodeCreateRequest.setSchemaName(SCHEMA_NAME);
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("value" + i));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		}

		// Create nodes with the same name
		for (int i = 0; i < 10; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(rootFolderUuid);
			nodeCreateRequest.setSchemaName(SCHEMA_NAME);
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("sameValue"));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		}

		waitForSearchIdleEvent();

		// Before update
		assertEquals(10, call(() -> client().searchNodes(PROJECT_NAME, queryName("sameValue"))).getMetainfo().getTotalCount());

		// 4. Update schema - Add the segment field for name
		SchemaUpdateRequest schemaUpdateRequest = new SchemaUpdateRequest();
		schemaUpdateRequest.setName(SCHEMA_NAME);
		schemaUpdateRequest.getFields().add(FieldUtil.createStringFieldSchema("name").setElasticsearch(IndexOptionHelper.getRawFieldOption()));
		schemaUpdateRequest.setSegmentField("name");
		grantAdminRole();

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaResponse.getUuid(), schemaUpdateRequest));
		}, JobStatus.COMPLETED, 1);

		waitForSearchIdleEvent();

		// After update
		assertEquals(1, call(() -> client().searchNodes(PROJECT_NAME, queryName("sameValue"))).getMetainfo().getTotalCount());
		for (int i = 0; i < 9; i++) {
			String expectedName = "sameValue_" + (i + 1);
			assertEquals("Could not find expected name {" + expectedName + "}", 1,
				call(() -> client().searchNodes(PROJECT_NAME, queryName(expectedName))).getMetainfo().getTotalCount());
		}

		// Now try to create conflicting node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(rootFolderUuid);
		nodeCreateRequest.setSchemaName(SCHEMA_NAME);
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("sameValue"));
		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest), CONFLICT, "node_conflicting_segmentfield_update", "name", "sameValue");

		// Assert job warnings
		JobListResponse jobs = call(() -> client().findJobs());
		JobResponse job = jobs.getData().get(0);
		assertEquals("node-conflict-resolution", job.getWarnings().get(0).getType());

	}

	@Test
	public void testSearchAfterSchemaUpdate() throws Exception {
		recreateIndices();

		String query = getSimpleTermQuery("schema.name.raw", "content");
		long oldCount, newCount;

		oldCount = call(() -> client().searchNodes(PROJECT_NAME, query)).getMetainfo().getTotalCount();

		SchemaResponse schema = call(() -> client().findSchemas(PROJECT_NAME)).getData().stream().filter(it -> it.getName().equals("content"))
			.findAny().get();
		List<FieldSchema> fields = schema.getFields();
		fields.add(new StringFieldSchemaImpl().setName("test").setLabel("Test"));

		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));

		// Wait for migration to complete
		waitForJobs(() -> {
			SchemaUpdateRequest updateRequest = new SchemaUpdateRequest().setFields(fields).setName(schema.getName());
			call(() -> client().updateSchema(schema.getUuid(), updateRequest, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(true)));
		}, COMPLETED, 1);

		waitForSearchIdleEvent();

		// Now search again and verify that we still find the same amount of elements
		newCount = call(() -> client().searchNodes(PROJECT_NAME, query)).getMetainfo().getTotalCount();

		assertThat(newCount).isEqualTo(oldCount);
	}

	@Test
	@Category({FailingTest.class})
	public void searchDuringMigration() throws Exception {
		grantAdminRole();
		String query = getSimpleTermQuery("schema.name.raw", "folder");

		recreateIndices();

		waitForSearchIdleEvent(() -> {
			NodeResponse parent = createNode();
			// Create some nodes for load during migration
			IntStream.range(0, 1000).forEach(i -> createNode(parent));
		});

		NodeListResponse beforeMigration = client().searchNodes(query).blockingGet();

		waitForLatestJob(() -> {
			migrateSchema("folder", false).blockingAwait();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			NodeListResponse duringMigration = client().searchNodes(query, new SearchParametersImpl().setWait(false)).blockingGet();

			assertThat(beforeMigration.getMetainfo().getTotalCount())
				.isEqualTo(duringMigration.getMetainfo().getTotalCount())
				.as("All nodes must be found during migration");
		});

		waitForSearchIdleEvent();
	}
}
