package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getRangeQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeBinarySearchTest extends AbstractNodeSearchEndpointTest {

	public NodeBinarySearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testBinarySearchMapping() throws Exception {
		grantAdmin();
		HibNode nodeA = content("concorde");
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
			String indexName = ContentDao.composeIndexName(projectUuid(), initialBranchUuid(),
				schemaVersionUuid, ContainerType.DRAFT);
			String id = ContentDao.composeDocumentId(nodeA.getUuid(), "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertEquals("Lorem ipsum dolor sit amet",
				doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content"));
			tx.success();
		}

		// file.content
		NodeListResponse response = call(
			() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.binary.file.content", "Lorem ipsum dolor sit amet")));
		assertEquals("Exactly one node should be found for the given content.", 1, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.file.content.raw", "Lorem ipsum dolor sit amet")));
		assertEquals("Exactly one node should be found for the given content.", 1, response.getData().size());

		// mimeType
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "application/rtf")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType.raw", "application/rtf")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

	@Test
	public void testGeolocationSearch() throws Exception {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		List<String> images = Arrays.asList("android-gps.jpg", "iphone-gps.jpg", "android-africa-gps.jpg");
		for (String image : images) {
			InputStream ins = getClass().getResourceAsStream("/pictures/" + image);
			byte[] bytes = IOUtils.toByteArray(ins);
			Buffer buffer = Buffer.buffer(bytes);

			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
			nodeCreateRequest.setSchemaName("binary_content");
			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary",
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), image, "image/jpeg"));
		}
		waitForSearchIdleEvent();
		String query = getESText("geosearch.es");
		NodeListResponse result = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(result.getData()).hasSize(2);
		// Only the two images should be found
		List<String> foundImages = result.getData().stream().map(n -> n.getFields().getBinaryField("binary").getFileName())
			.collect(Collectors.toList());
		assertTrue(foundImages.contains("android-gps.jpg"));
		assertTrue(foundImages.contains("android-africa-gps.jpg"));
	}

	@Test
	public void testDocumentSearch() throws Exception {
		grantAdmin();
		HibNode nodeA = content("concorde");
		String nodeUuid = tx(() -> nodeA.getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add binary field
		SchemaUpdateRequest schemaUpdateRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		schemaUpdateRequest.addField(new BinaryFieldSchemaImpl().setName("binary"));
		waitForJobs(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		}, JobStatus.COMPLETED, 1);

		byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
		call(
			() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binary", new ByteArrayInputStream(bytes), bytes.length,
				"test.rtf", "application/rtf"));

		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			String indexName = ContentDao.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeA.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = ContentDao.composeDocumentId(nodeUuid, "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertEquals("Lorem ipsum dolor sit amet",
				doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content"));
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
		grantAdmin();
		HibNode nodeA = content("concorde");
		String nodeUuid = tx(() -> nodeA.getUuid());
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add binary field
		SchemaUpdateRequest schemaUpdateRequest = call(() -> client().findSchemaByUuid(contentSchemaUuid)).toUpdateRequest();
		schemaUpdateRequest.addField(new BinaryFieldSchemaImpl().setName("binary"));
		waitForJobs(() -> {
			call(() -> client().updateSchema(contentSchemaUuid, schemaUpdateRequest));
		}, JobStatus.COMPLETED, 1);

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
