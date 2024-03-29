package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeSearchEndpointATest extends AbstractNodeSearchEndpointTest {

	public NodeSearchEndpointATest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSearchPublishedNodes() throws Exception {
		recreateIndices();

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent)));

		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db().tx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		waitForSearchIdleEvent();

		// "supersonic" found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent), new VersioningParametersImpl()
			.published()));

		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// Change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		waitForSearchIdleEvent();

		// "supersonic" still found, "urschnell" not found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent), new VersioningParametersImpl()
			.published()));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent), new VersioningParametersImpl()
			.published()));
		assertThat(response.getData()).as("Published search result").isEmpty();

		// publish content "urschnell"
		call(() -> client().publishNode(PROJECT_NAME, db().tx(() -> content("concorde").getUuid())));

		waitForSearchIdleEvent();

		// "supersonic" no longer found, but "urschnell" found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	/**
	 * Assert that branchUuid field can be used for searches.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSearchWithBranchUuid() throws Exception {
		recreateIndices();
		grantAdmin();

		String query = getSimpleTermQuery("branchUuid", initialBranchUuid());
		long initialCount = call(() -> client().searchNodes(PROJECT_NAME, query)).getMetainfo().getTotalCount();

		waitForLatestJob(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setName("extraBranch");
			branchCreateRequest.setLatest(false);
			branchCreateRequest.setBaseBranch(new BranchReference().setUuid(initialBranchUuid()));
			call(() -> client().createBranch(PROJECT_NAME, branchCreateRequest));
		});

		long afterCount = call(() -> client().searchNodes(PROJECT_NAME, query)).getMetainfo().getTotalCount();
		assertEquals("The amount of hits should not change after creating a new branch",afterCount, initialCount);
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
		grantAdmin();

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
}
