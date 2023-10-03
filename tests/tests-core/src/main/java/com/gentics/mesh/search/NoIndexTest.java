package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getUuidQuery;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NoIndexTest extends AbstractNodeSearchEndpointTest {

	public NoIndexTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	protected void setNoFieldIndexing(String fieldName, boolean disable) {
		tx(() -> {
			HibNode nodeTmp = content("concorde");
			SchemaModel schema = nodeTmp.getSchemaContainer().getLatestVersion().getSchema();
			schema.getField(fieldName).setNoIndex(disable);
			actions().updateSchemaVersion(nodeTmp.getSchemaContainer().getLatestVersion());
			return nodeTmp;
		});
	}

	protected void setNoSchemaIndexing(boolean disable) {
		tx(() -> {
			HibNode nodeTmp = content("concorde");
			SchemaModel schema = nodeTmp.getSchemaContainer().getLatestVersion().getSchema();
			schema.setNoIndex(disable);
			actions().updateSchemaVersion(nodeTmp.getSchemaContainer().getLatestVersion());
			return nodeTmp;
		});
	}

	@Test
	public void testNoFieldIndex() throws Exception {
		String uuid = db().tx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));

		// Concorde exists in the index
		recreateIndices();
		String oldContent = "supersonic";
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		setNoFieldIndexing("content", true);

		// No more Concorde available
		recreateIndices();
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Search result").isEmpty();

		// Control case - the node itself is found by its UUID
		response = call(() -> client().searchNodes(PROJECT_NAME, getUuidQuery(uuid)));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testNoSchemaIndex() throws Exception {
		String uuid = db().tx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));

		// Concorde exists in the field index
		recreateIndices();
		String oldContent = "supersonic";
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		setNoSchemaIndexing(true);

		// No more Concorde available through the field
		recreateIndices();
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", oldContent)));
		assertThat(response.getData()).as("Search result").isEmpty();

		// Control case - the node itself is not found by its UUID
		response = call(() -> client().searchNodes(PROJECT_NAME, getUuidQuery(uuid)));
		assertThat(response.getData()).as("Search result").isEmpty();
	}
}
