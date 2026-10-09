package com.gentics.mesh.search;

import static com.gentics.mesh.test.AWSTestMode.RUSTFS;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = MeshCoreOptionChanger.ES_STRICT_MODE, awsContainer = RUSTFS)
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

	@Test
	public void testSearchBinaryFieldCustomMapping() throws Exception {
		addFieldWithCustomMapping(new BinaryFieldSchemaImpl().setName("binary"));
		String nodeUuid = tx(() -> content().getUuid());

		byte[] bytes = "Lorem ipsum dolor sit amet".getBytes();
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeUuid, "en", "draft", "binary", new ByteArrayInputStream(bytes), bytes.length,
			"Test File.TXT", "text/plain"));
		waitForSearchIdleEvent();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.filename", "Test File.TXT"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given binary filename.", 1, response.getData().size());
		assertEquals(nodeUuid, response.getData().get(0).getUuid());
	}

	@Test
	public void testSearchS3BinaryFieldCustomMapping() throws Exception {
		addFieldWithCustomMapping(new S3BinaryFieldSchemaImpl().setName("s3binary"));
		String nodeUuid = tx(() -> content().getUuid());
		String version = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft())).getVersion();

		call(() -> client().updateNodeS3BinaryField(PROJECT_NAME, nodeUuid, "s3binary",
			new S3BinaryUploadRequest().setFilename("Test Image.JPG").setLanguage("en").setVersion(version)));
		waitForSearchIdleEvent();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.s3binary.filename", "Test Image.JPG"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given s3binary filename.", 1, response.getData().size());
		assertEquals(nodeUuid, response.getData().get(0).getUuid());
	}

	/**
	 * Add the given (s3)binary field to the content schema, with a custom mapping, which only maps the filename.
	 * The filename is mapped as keyword, so the exact (mixed case, containing spaces) filename is only found, when the custom mapping has been
	 * applied, and not by a dynamically mapped (analyzed) text field.
	 *
	 * @param field
	 */
	private void addFieldWithCustomMapping(FieldSchema field) {
		String schemaUuid = tx(() -> content().getSchemaContainer().getUuid());
		SchemaUpdateRequest request = tx(() -> JsonUtil.readValue(content().getSchemaContainer().getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		JsonObject filenameMapping = new JsonObject().put("type", "keyword").put("index", true);
		field.setElasticsearch(new JsonObject()
			.put("type", "object")
			.put("dynamic", false)
			.put("properties", new JsonObject().put("filename", filenameMapping)));
		request.addField(field);

		grantAdmin();
		waitForJob(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		});
		revokeAdmin();
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
