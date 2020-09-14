package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.BinaryFieldParserOption.DEFAULT;
import static com.gentics.mesh.core.rest.schema.BinaryFieldParserOption.NONE;
import static com.gentics.mesh.core.rest.schema.BinaryFieldParserOption.PARSE_AND_SEARCH;
import static com.gentics.mesh.core.rest.schema.BinaryFieldParserOption.PARSE_ONLY;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.schema.BinaryFieldParserOption;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = true, elasticsearch = ElasticsearchTestMode.TRACKING)
public class BinaryFieldParserOptionTest extends AbstractMeshTest {
	private NodeResponse nodeResponse;
	private BinaryField binaryField;
	private BinaryMetadata metadata;
	private JsonObject document;

	private void setUp(BinaryFieldParserOption parserOption) throws Exception {
		// Make sure all options are tested in case more gets added in the future
		assertThat(BinaryFieldParserOption.values())
			.containsExactlyInAnyOrder(DEFAULT, NONE, PARSE_ONLY, PARSE_AND_SEARCH);

		SchemaResponse oldSchema = getSchemaByName("binary_content");
		SchemaUpdateRequest binarySchema = oldSchema.toUpdateRequest();
		binarySchema.getField("binary", BinaryFieldSchemaImpl.class)
			.setParserOption(parserOption);
//		updateAndMigrateSchema(oldSchema, binarySchema);
		
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
		document = trackingSearchProvider().getLatestStoreEvent(nodeResponse.getUuid());
	}

	@Test
	public void testDefault() throws Exception {
		// Same as PARSE_AND_SEARCh
		setUp(DEFAULT);

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}

	@Test
	public void testNone() throws Exception {
		setUp(NONE);

		assertThat(metadata).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata")).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file")).isNull();
	}

	@Test
	public void testParseOnly() throws Exception {
		setUp(PARSE_ONLY);

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata")).isNull();
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file")).isNull();
	}

	@Test
	public void testParseAndSearch() throws Exception {
		setUp(PARSE_AND_SEARCH);

		assertThat(metadata.get("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("metadata").getString("subject")).isEqualTo("TestSubject");
		assertThat(document.getJsonObject("fields").getJsonObject("binary").getJsonObject("file").getString("content")).isEqualTo("Das ist ein Word Dokument für den Johannes");
	}
}
