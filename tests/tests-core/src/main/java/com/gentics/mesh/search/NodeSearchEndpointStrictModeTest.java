package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = MeshCoreOptionChanger.ES_STRICT_MODE)
public class NodeSearchEndpointStrictModeTest extends AbstractNodeSearchEndpointTest {

	public NodeSearchEndpointStrictModeTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSearchStringFieldRaw() throws Exception {
		addCustomMapping(true);
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.teaser", "Concorde_english_name"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());
	}


	@Test
	public void testSetupStrictNoMapping() throws Exception {
		addCustomMapping(false);
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.teaser", "Concorde_english_name"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 0, response.getData().size());
	}

	private void addCustomMapping(boolean valid) {
		String schemaUuid = tx(() -> content().getSchemaContainer().getUuid());
		SchemaUpdateRequest request = tx(() -> JsonUtil.readValue(content().getSchemaContainer().getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		if (valid) {
			JsonObject keywordMapping = new JsonObject().put("index", true).put("type", "keyword");
			request.getField("teaser").setElasticsearch(keywordMapping);
		} else {
			request.getField("teaser").setElasticsearch(new JsonObject().put("_meshLanguageOverride", new JsonObject()));
		}

		grantAdmin();
		waitForJob(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		});
		revokeAdmin();

	}

}
