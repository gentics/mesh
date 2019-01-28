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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true, withIngestPlugin = true)
public class NodeBinarySearchTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testBinarySearchMapping() throws Exception {
		try (Tx tx = tx()) {
			Node nodeA = content("concorde");
			Node nodeB = content();
			SchemaModel schema = nodeA.getSchemaContainer().getLatestVersion().getSchema();

			List<String> names = Arrays.asList("binary", "binary2", "binary3");
			for (String name : names) {
				BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
				binaryField.setName(name);
				JsonObject customMapping = new JsonObject();
				customMapping.put("mimeType", IndexOptionHelper.getRawFieldOption());
				customMapping.put("file.content", IndexOptionHelper.getRawFieldOption());
				binaryField.setElasticsearch(customMapping);
				schema.addField(binaryField);
			}

			nodeA.getSchemaContainer().getLatestVersion().setSchema(schema);

			// image
			Binary binaryA = MeshInternal.get().boot().binaryRoot().create("someHashA", 200L);
			binaryA.setImageHeight(200);
			binaryA.setImageWidth(400);
			nodeA.getLatestDraftFieldContainer(english()).createBinary("binary", binaryA).setFileName("somefile.jpg").setMimeType("image/jpeg")
				.setImageDominantColor("#super");

			// file
			Binary binaryB = MeshInternal.get().boot().binaryRoot().create("someHashB", 200L);
			byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
			MeshInternal.get().binaryStorage().store(Flowable.fromArray(Buffer.buffer(bytes)), binaryB.getUuid()).blockingAwait();

			nodeB.getLatestDraftFieldContainer(english()).createBinary("binary", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");
			nodeB.getLatestDraftFieldContainer(english()).createBinary("binary2", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");

			recreateIndices();

			String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeB.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = NodeGraphFieldContainer.composeDocumentId(nodeB.getUuid(), "en");
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
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "text/plain")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType.raw", "text/plain")));
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
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()), buffer.length(), image, "image/jpeg"));
		}
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
	public void testSearchBinaryField() throws Exception {
		try (Tx tx = tx()) {
			Node nodeA = content("concorde");
			Node nodeB = content();
			SchemaModel schema = nodeA.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new BinaryFieldSchemaImpl().setName("binary"));
			nodeA.getSchemaContainer().getLatestVersion().setSchema(schema);

			// image
			Binary binaryA = MeshInternal.get().boot().binaryRoot().create("someHashA", 200L);
			binaryA.setImageHeight(200);
			binaryA.setImageWidth(400);
			nodeA.getLatestDraftFieldContainer(english()).createBinary("binary", binaryA).setFileName("somefile.jpg").setMimeType("image/jpeg")
				.setImageDominantColor("#super");

			// file
			Binary binaryB = MeshInternal.get().boot().binaryRoot().create("someHashB", 200L);
			BinaryGraphField binary = nodeB.getLatestDraftFieldContainer(english()).createBinary("binary", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");
			byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
			MeshInternal.get().binaryStorage().store(Flowable.fromArray(Buffer.buffer(bytes)), binary.getBinary().getUuid()).blockingAwait();
			recreateIndices();

			String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeB.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = NodeGraphFieldContainer.composeDocumentId(nodeB.getUuid(), "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertEquals("Lorem ipsum dolor sit amet",
				doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content"));
			tx.success();
		}

		// filesize
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.filesize", 100, 300),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly two nodes should be found for the given filesize range.", 2, response.getData().size());

		// width
		response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.width", 300, 500), new VersioningParametersImpl()
			.draft()));
		assertEquals("Exactly one node should be found for the given image width range.", 1, response.getData().size());

		// height
		response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.height", 100, 300), new VersioningParametersImpl()
			.draft()));
		assertEquals("Exactly one node should be found for the given image height range.", 1, response.getData().size());

		// dominantColor
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.dominantColor", "#super"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given image dominant color.", 1, response.getData().size());

		// mimeType
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "image/jpeg"),
			new VersioningParametersImpl().draft()));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

}
