package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshOptionChanger.EXCLUDE_BINARY_SEARCH;
import static com.gentics.mesh.test.context.MeshTestHelper.getRangeQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.category.FailingTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

import io.vertx.core.json.JsonObject;

/**
 * Test search index handling with disabled include binary fields option.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = EXCLUDE_BINARY_SEARCH)
public class NodeBinaryDisabledSearchTest extends AbstractNodeSearchEndpointTest {

	public NodeBinaryDisabledSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	@Category({FailingTest.class})
	public void testBinarySearchMapping() throws Exception {
		grantAdminRole();
		Node nodeA = content("concorde");
		String nodeUuid = tx(() -> nodeA.getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Update the schema to include the binary fields we need
		SchemaUpdateRequest schemaUpdateRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		List<String> names = Arrays.asList("binary", "binary2", "binary3");
		for (String name : names) {
			BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
			binaryField.setName(name);
			JsonObject customMapping = new JsonObject();
			customMapping.put("mimeType", IndexOptionHelper.getRawFieldOption());
			customMapping.put("file.content", IndexOptionHelper.getRawFieldOption());
			binaryField.setElasticsearch(customMapping);
			schemaUpdateRequest.addField(binaryField);
		}
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		});
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		});

		// .rtf with lorem text
		byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
		call(
			() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binary", new ByteArrayInputStream(bytes), bytes.length,
				"test.rtf", "application/rtf"));
		call(
			() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binary2", new ByteArrayInputStream(bytes), bytes.length,
				"test.rtf", "application/rtf"));

		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			String schemaVersionUuid = nodeA.getSchemaContainer().getLatestVersion().getUuid();
			String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
				schemaVersionUuid, ContainerType.DRAFT);
			String id = NodeGraphFieldContainer.composeDocumentId(nodeA.getUuid(), "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertFalse("The information should not have been added to the search document.",
				doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").containsKey("file"));
			tx.success();
		}

		// file.content
		NodeListResponse response = call(
			() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.binary.file.content", "Lorem ipsum dolor sit amet")));
		assertEquals("No node should be found.", 0, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.file.content.raw", "Lorem ipsum dolor sit amet")));
		assertEquals("No node should be found.", 0, response.getData().size());

		// mimeType
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "application/rtf")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType.raw", "application/rtf")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

	@Test
	public void testDocumentSearch() throws Exception {
		grantAdminRole();
		Node nodeA = content("concorde");
		String nodeUuid = tx(() -> nodeA.getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add binary field
		SchemaUpdateRequest schemaUpdateRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		schemaUpdateRequest.addField(new BinaryFieldSchemaImpl().setName("binary"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		});

		byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
		call(
			() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binary", new ByteArrayInputStream(bytes), bytes.length,
				"test.rtf", "application/rtf"));

		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeA.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = NodeGraphFieldContainer.composeDocumentId(nodeUuid, "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertFalse("The binary content should not be part of the document",
				doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").containsKey("file"));
			tx.success();
		}

		// filesize
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.filesize", 40, 50),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given filesize range.", 1, response.getData().size());

		// mimeType
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "application/rtf"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

	@Test
	public void testImageSearch() throws IOException {
		grantAdminRole();
		Node nodeA = content("concorde");
		String nodeUuid = tx(() -> nodeA.getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add binary field
		SchemaUpdateRequest schemaUpdateRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		schemaUpdateRequest.addField(new BinaryFieldSchemaImpl().setName("binary"));
		waitForJob(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		});

		// Upload image
		byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/pictures/blume.jpg"));
		call(
			() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binary", new ByteArrayInputStream(bytes), bytes.length,
				"blume.jpg", "image/jpeg"));

		waitForSearchIdleEvent();

		// width
		NodeListResponse response = call(
			() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.width", 1150, 1170), new VersioningParametersImpl()
				.draft()));
		assertEquals("Exactly one node should be found for the given image width range.", 1, response.getData().size());

		// height
		response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.height", 1276, 1476), new VersioningParametersImpl()
			.draft()));
		assertEquals("Exactly one node should be found for the given image height range.", 1, response.getData().size());

		// dominantColor
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.dominantColor", "#737042"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given image dominant color.", 1, response.getData().size());

	}
}
