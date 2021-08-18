package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getRangeQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibBinaryField;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeBinaryDocumentSearchTest extends AbstractNodeSearchEndpointTest {

	public NodeBinaryDocumentSearchTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testBinarySearchMapping() throws Exception {
		HibNode nodeA = content("concorde");
		HibNode nodeB = content();

		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
			SchemaVersionModel schema = nodeA.getSchemaContainer().getLatestVersion().getSchema();

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
			HibBinary binaryA = ((GraphDBTx) tx).binaries().create("someHashA", 200L).runInExistingTx(tx);
			binaryA.setImageHeight(200);
			binaryA.setImageWidth(400);
			contentDao.getLatestDraftFieldContainer(nodeA, english()).createBinary("binary", binaryA).setFileName("somefile.jpg").setMimeType("image/jpeg")
				.setImageDominantColor("#super");

			// file
			HibBinary binaryB = ((GraphDBTx) tx).binaries().create("someHashB", 200L).runInExistingTx(tx);
			byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
			mesh().binaryStorage().store(Flowable.fromArray(Buffer.buffer(bytes)), binaryB.getUuid()).blockingAwait();

			contentDao.getLatestDraftFieldContainer(nodeB, english()).createBinary("binary", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");
			contentDao.getLatestDraftFieldContainer(nodeB, english()).createBinary("binary2", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");
			tx.success();
		}

		recreateIndices();

		try (Tx tx = tx()) {
			String indexName = ContentDao.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeB.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = ContentDao.composeDocumentId(nodeB.getUuid(), "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertFalse(doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").containsKey("file"));
			tx.success();
		}

		// file.content
		NodeListResponse response = call(
			() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.binary.file.content", "Lorem ipsum dolor sit amet")));
		assertEquals("No content should be found for the given content since the field was not added to the index.", 0, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.file.content.raw", "Lorem ipsum dolor sit amet")));
		assertEquals("Exactly one node should be found for the given content.", 0, response.getData().size());

		// mimeType
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "text/plain")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType.raw", "text/plain")));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

	@Test
	public void testSearchBinaryField() throws Exception {
		HibNode nodeA = content("concorde");
		HibNode nodeB = content();

		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
			SchemaVersionModel schema = nodeA.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new BinaryFieldSchemaImpl().setName("binary"));
			nodeA.getSchemaContainer().getLatestVersion().setSchema(schema);

			// image
			HibBinary binaryA = ((GraphDBTx) tx).binaries().create("someHashA", 200L).runInExistingTx(tx);
			binaryA.setImageHeight(200);
			binaryA.setImageWidth(400);
			contentDao.getLatestDraftFieldContainer(nodeA, english()).createBinary("binary", binaryA).setFileName("somefile.jpg").setMimeType("image/jpeg")
				.setImageDominantColor("#super");

			// file
			HibBinary binaryB = ((GraphDBTx) tx).binaries().create("someHashB", 200L).runInExistingTx(tx);
			HibBinaryField binary = contentDao.getLatestDraftFieldContainer(nodeB, english()).createBinary("binary", binaryB).setFileName("somefile.dat")
				.setMimeType("text/plain");
			byte[] bytes = Base64.getDecoder().decode("e1xydGYxXGFuc2kNCkxvcmVtIGlwc3VtIGRvbG9yIHNpdCBhbWV0DQpccGFyIH0=");
			mesh().binaryStorage().store(Flowable.fromArray(Buffer.buffer(bytes)), binary.getBinary().getUuid()).blockingAwait();
			tx.success();
		}

		recreateIndices();

		try (Tx tx = tx()) {
			String indexName = ContentDao.composeIndexName(projectUuid(), initialBranchUuid(),
				nodeB.getSchemaContainer().getLatestVersion().getUuid(), ContainerType.DRAFT);
			String id = ContentDao.composeDocumentId(nodeB.getUuid(), "en");
			JsonObject doc = getProvider().getDocument(indexName, id).blockingGet();
			assertFalse(doc.getJsonObject("_source").getJsonObject("fields").getJsonObject("binary").containsKey("file"));
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
