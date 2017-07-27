package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.VersionNumber;

import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class BinaryFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "binaryField";

	/**
	 * Update the schema and add a binary field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		setSchema(false);
	}

	private void setSchema(boolean isRequired) {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();

			// add non restricted string field
			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName(FIELD_NAME);
			binaryFieldSchema.setLabel("Some label");
			binaryFieldSchema.setRequired(isRequired);
			schema.addField(binaryFieldSchema);

			schemaContainer("folder").getLatestVersion().setSchema(schema);
		}
	}

	@Override
	public void testReadNodeWithExistingField() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void testUpdateNodeFieldWithField() {
		// TODO Auto-generated method stub
	}

	@Test
	public void testVersionConflictUpload() {
		// 1. Upload a binary field
		String uuid = db().tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = db().tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
		NodeResponse responseA = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer,
				"filename.txt", "application/binary"));

		assertThat(responseA.getVersion()).doesNotMatch(version.toString());

		// Upload again - A conflict should be detected since we provide the original outdated version
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer, "filename.txt",
				"application/binary"), CONFLICT, "node_error_conflict_detected");

		// Now use the correct version and verify that the upload succeeds
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", responseA.getVersion(), FIELD_NAME, buffer, "filename.txt",
				"application/binary"));

	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = db().tx(() -> folder("2015").getUuid());
			Buffer buffer = TestUtils.randomBuffer(1000);
			VersionNumber version = db().tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer, "filename.txt",
					"application/binary"));

			NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("draft")));
			assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
			String oldVersion = firstResponse.getVersion();
			BinaryField binaryField = firstResponse.getFields().getBinaryField(FIELD_NAME);

			// 2. Update the node using the loaded binary field data
			NodeResponse secondResponse = updateNode(FIELD_NAME, binaryField);
			assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
			assertThat(secondResponse.getVersion()).as("New version number should not be generated.").isEqualTo(oldVersion);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		String filename = "filename.txt";
		Buffer buffer = TestUtils.randomBuffer(1000);
		Node node = folder("2015");

		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = db().tx(() -> folder("2015").getUuid());
			VersionNumber version = db().tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());

			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer, filename,
					"application/binary"));

			NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
			String oldVersion = firstResponse.getVersion();

			// 2. Set the field to null
			NodeResponse secondResponse = updateNode(FIELD_NAME, null);
			assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNull();
			assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

			// Assert that the old version was not modified
			node.reload();
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getBinary(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getBinary(FIELD_NAME)).isNotNull();
			String oldFilename = latest.getPreviousVersion().getBinary(FIELD_NAME).getFileName();
			assertThat(oldFilename).as("Old version filename should match the intitial version filename").isEqualTo(filename);

			// 3. Set the field to null one more time and assert that no new version was created
			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
					secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = db().tx(() -> folder("2015").getUuid());
			Buffer buffer = TestUtils.randomBuffer(1000);
			VersionNumber version = db().tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer, "filename.txt",
					"application/binary"));

			NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("draft")));
			assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
			String oldVersion = firstResponse.getVersion();

			// 2. Set the field to empty - Node should not be updated since nothing changes
			NodeResponse secondResponse = updateNode(FIELD_NAME, new BinaryFieldImpl());
			assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
		}
	}

	@Test
	public void testUpdateSetEmptyFilename() {
		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = db().tx(() -> folder("2015").getUuid());
			Buffer buffer = TestUtils.randomBuffer(1000);
			VersionNumber version = db().tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME, buffer, "filename.txt",
					"application/binary"));

			NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("draft")));
			assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());

			// 2. Set the field to empty
			updateNodeFailure(FIELD_NAME, new BinaryFieldImpl().setFileName(""), BAD_REQUEST, "field_binary_error_emptyfilename", FIELD_NAME);
			updateNodeFailure(FIELD_NAME, new BinaryFieldImpl().setMimeType(""), BAD_REQUEST, "field_binary_error_emptymimetype", FIELD_NAME);
		}
	}

	@Override
	public void testCreateNodeWithField() {
		// TODO Auto-generated method stub
	}

	@Override
	public void testCreateNodeWithNoField() {
		// TODO Auto-generated method stub
	}

}
