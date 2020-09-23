package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.CollectionUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = true, elasticsearch = ElasticsearchTestMode.TRACKING)
public class BinaryFieldExtractOptionTest extends AbstractMeshTest {
	private NodeResponse nodeResponse;
	private BinaryField binaryField;
	private BinaryMetadata metadata;
	private JsonObject document;
	private String plainText;

	private void setUp(BinaryExtractOptions extract) throws Exception {
		setUp(extract, null);
	}

	private void setUp(BinaryExtractOptions extract, Set<String> whitelist) throws Exception {
		if (extract != null) {
			SchemaResponse oldSchema = getSchemaByName("binary_content");
			SchemaUpdateRequest binarySchema = oldSchema.toUpdateRequest();
			binarySchema.getField("binary", BinaryFieldSchemaImpl.class)
				.setBinaryExtractOptions(extract);
			updateAndMigrateSchema(oldSchema, binarySchema);
		}

		if (whitelist != null) {
			mesh().options().getUploadOptions().setMetadataWhitelist(whitelist);
		}

		// UPLOAD
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		Buffer buffer = getBuffer("/testfiles/test.docx");
		NodeResponse node = createBinaryNode(parentNodeUuid);
		nodeResponse = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
				buffer.length(), "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
		binaryField = nodeResponse.getFields().getBinaryField("binary");
		waitForSearchIdleEvent();

		metadata = binaryField.getMetadata();
		plainText = binaryField.getPlainText();
		document = trackingSearchProvider().getLatestStoreEvent(nodeResponse.getUuid());
	}

	@Test
	public void testDefault() throws Exception {
		// Same as both
		setUp(null);

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(metadata.getMap()).hasSize(11);
		assertThat(plainText).isEqualTo("Das ist ein Word Dokument für den Johannes");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}

	@Test
	public void testNone() throws Exception {
		setUp(new BinaryExtractOptions(false, false));

		assertThat(metadata).isEmpty();
		assertThat(plainText).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata")).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file")).isNull();
	}

	@Test
	public void testMetadata() throws Exception {
		setUp(new BinaryExtractOptions(false, true));

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(metadata.getMap()).hasSize(11);
		assertThat(plainText).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file")).isNull();
	}

	@Test
	public void testContent() throws Exception {
		setUp(new BinaryExtractOptions(true, false));

		assertThat(metadata).isEmpty();
		assertThat(plainText).isEqualTo("Das ist ein Word Dokument für den Johannes");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata")).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}

	@Test
	public void testBoth() throws Exception {
		setUp(new BinaryExtractOptions(true, true));

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(metadata.getMap()).hasSize(11);
		assertThat(plainText).isEqualTo("Das ist ein Word Dokument für den Johannes");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}

	@Test
	public void testWhitelist() throws Exception {
		setUp(new BinaryExtractOptions(false, true), CollectionUtil.setOf("subject"));

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(plainText).isNull();
		assertThat(metadata.getLocation()).isNull();
		assertThat(metadata.getMap()).hasSize(1);

		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}
}
