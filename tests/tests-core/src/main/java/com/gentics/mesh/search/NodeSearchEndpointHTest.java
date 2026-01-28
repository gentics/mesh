package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = NodeSearchEndpointHTest.OptionChanger.class)
public class NodeSearchEndpointHTest extends AbstractNodeSearchEndpointTest {

	protected static int BATCH_SIZE = 80;

	protected boolean needsReset = true;

	public NodeSearchEndpointHTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Before
	public void resetIfNeeded() {
		if (needsReset) {
			String source = tx(() -> content("concorde").getUuid());
			String schema = tx(() -> content("concorde").getSchemaContainer().getName());
			String parent = tx(() -> content("concorde").getParentNode(initialBranchUuid()).getUuid());
			NodeResponse sourceNode = call(() -> client().findNodeByUuid(PROJECT_NAME, source));
			for (int i = 0; i < BATCH_SIZE; i++) {
				NodeCreateRequest request = new NodeCreateRequest();
				request.setLanguage(sourceNode.getLanguage());
				request.setSchemaName(schema);
				request.setParentNodeUuid(parent);
				request.setFields(sourceNode.getFields());
				request.getFields().put("slug", request.getFields().getStringField("slug").setString(i + "_slug" + System.currentTimeMillis() + ".html"));
				call(() -> client().createNode(PROJECT_NAME, request));
			}
			waitForSearchIdleEvent();
			needsReset = false;
		}
	}

	@Test
	public void testSyncBatchSize() {
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"), new VersioningParametersImpl().draft()));
		assertThat(response.getMetainfo().getTotalCount()).isEqualTo(BATCH_SIZE / 4);
	}

	public static final class OptionChanger implements MeshOptionChanger {
		@Override
		public void change(MeshOptions options) {
			options.getSearchOptions().setSyncFetchBatchSize(BATCH_SIZE / 4);
		}
	}
}
