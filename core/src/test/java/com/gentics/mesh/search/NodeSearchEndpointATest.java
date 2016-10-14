package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;

public class NodeSearchEndpointATest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testReindexNodeIndex() throws Exception {

		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db.noTx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// // Add the user to the admin group - this way the user is in fact an admin.
		try (NoTx noTrx = db.noTx()) {
			user().addGroup(groups().get("admin"));
		}

		// Now clear all data
		searchProvider.clear();

		GenericMessageResponse message = call(() -> getClient().invokeReindex());
		expectResponseMessage(message, "search_admin_reindex_invoked");

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

	}

	@Test
	public void testSearchPublishedNodes() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db.noTx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> getClient().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		// "supersonic" still found, "urschnell" not found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		// publish content "urschnell"
		call(() -> getClient().publishNode(PROJECT_NAME, db.noTx(() -> content("concorde").getUuid())));

		// "supersonic" no longer found, but "urschnell" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

}
