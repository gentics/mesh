package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class NodeSearchEndpointATest extends AbstractNodeSearchEndpointTest {

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

}
