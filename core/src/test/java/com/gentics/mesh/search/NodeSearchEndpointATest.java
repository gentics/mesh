package com.gentics.mesh.search;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class NodeSearchEndpointATest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testReindexNodeIndex() throws Exception {

		try (Tx tx = tx()) {
			recreateIndices();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";

		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db().tx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// // Add the user to the admin group - this way the user is in fact an admin.
		try (Tx tx = tx()) {
			user().addGroup(data().getGroups().get("admin"));
			tx.success();
		}

		// Now clear all data
		searchProvider().clear().blockingAwait();

		GenericMessageResponse message = call(() -> client().invokeReindex());
		assertMessage(message, "search_admin_reindex_invoked");

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

	}

	@Test
	public void testSearchPublishedNodes() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent)));
		System.out.println(response.toJson());
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db().tx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent), new VersioningParametersImpl()
				.published()));
		System.out.println(response.toJson());
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// Change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		// "supersonic" still found, "urschnell" not found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent), new VersioningParametersImpl()
				.published()));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent), new VersioningParametersImpl()
				.published()));
		assertThat(response.getData()).as("Published search result").isEmpty();

		// publish content "urschnell"
		call(() -> client().publishNode(PROJECT_NAME, db().tx(() -> content("concorde").getUuid())));

		// "supersonic" no longer found, but "urschnell" found in published nodes
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchAfterSchemaUpdate() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

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
			call(() -> client().updateSchema(schema.getUuid(), updateRequest, new SchemaUpdateParametersImpl().setUpdateAssignedReleases(true)));
		}, COMPLETED, 1);

		// Now search again and verify that we still find the same amount of elements
		newCount = call(() -> client().searchNodes(PROJECT_NAME, query)).getMetainfo().getTotalCount();

		assertThat(newCount).isEqualTo(oldCount);
	}
}
