package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.test.core.TestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
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
		String uuid = tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
		NodeResponse responseA = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
			"filename.txt", "application/binary"));

		assertThat(responseA.getVersion()).doesNotMatch(version.toString());

		// Upload again - A conflict should be detected since we provide the original outdated version
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"), CONFLICT, "node_error_conflict_detected");

		// Now use the correct version and verify that the upload succeeds
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", responseA.getVersion(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"));

	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = tx(() -> folder("2015").getUuid());
			Buffer buffer = TestUtils.randomBuffer(1000);
			VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
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

		// 1. Upload a binary field
		String uuid = tx(() -> folder("2015").getUuid());
		VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), filename, "application/binary"));

		NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		String oldVersion = firstResponse.getVersion();

		// 2. Set the field to null
		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getBinary(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getBinary(FIELD_NAME)).isNotNull();
			String oldFilename = latest.getPreviousVersion().getBinary(FIELD_NAME).getFileName();
			assertThat(oldFilename).as("Old version filename should match the intitial version filename").isEqualTo(filename);

			// 3. Set the field to null one more time and assert that no new version was created
			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse
				.getVersion());
		}
	}

	@Test
	public void testUpdateDelete() throws IOException {
		// 1. Upload a binary field
		NodeResponse response = createNodeWithField();

		// Clear the local binary storage directory to simulate a storage inconsistency
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory()));

		// 2. Delete the node
		call(() -> client().deleteNode(PROJECT_NAME, response.getUuid()));

	}

	@Test
	public void testDownloadBogusNames() {

		List<String> names = Arrays.asList("file", "file.", ".", "jpeg", "jpg", "JPG", "file.JPG", "file.PDF");
		String uuid = tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);

		for (String name : names) {
			// 1. Upload a binary field
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", "draft", FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), name,
				"application/pdf2"));

			MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, uuid, "en", FIELD_NAME));
			assertEquals("application/pdf2", response.getContentType());
			assertEquals(name, response.getFilename());
		}

	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		try (Tx tx = tx()) {
			// 1. Upload a binary field
			String uuid = tx(() -> folder("2015").getUuid());
			Buffer buffer = TestUtils.randomBuffer(1000);
			VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
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
		String uuid = tx(() -> folder("2015").getUuid());
		// 1. Upload a binary field
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"));

		NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setVersion("draft")));
		assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());

		// 2. Set the field to empty
		updateNodeFailure(FIELD_NAME, new BinaryFieldImpl().setFileName(""), BAD_REQUEST, "field_binary_error_emptyfilename", FIELD_NAME);
		updateNodeFailure(FIELD_NAME, new BinaryFieldImpl().setMimeType(""), BAD_REQUEST, "field_binary_error_emptymimetype", FIELD_NAME);
	}

	@Test
	public void testBinaryDisplayField() throws Exception {
		String fileName = "blume.jpg";
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);
		String parentUuid = tx(() -> folder("2015").getUuid());

		tx(() -> group().addRole(roles().get("admin")));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en").setParentNodeUuid(parentUuid).setSchemaName("binary_content");
		NodeResponse nodeResponse1 = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", nodeResponse1.getVersion(), "binary",
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), fileName,
			"application/binary"));

		SchemaResponse binarySchema = call(() -> client().findSchemas(PROJECT_NAME)).getData().stream().filter(s -> s.getName().equals(
			"binary_content")).findFirst().get();
		SchemaUpdateRequest schemaUpdateRequest = JsonUtil.readValue(binarySchema.toJson(), SchemaUpdateRequest.class);
		schemaUpdateRequest.setDisplayField("binary");
		waitForJobs(() -> {
			call(() -> client().updateSchema(binarySchema.getUuid(), schemaUpdateRequest));
		}, JobStatus.COMPLETED, 1);

		NodeResponse nodeResponse3 = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeResponse1.getUuid()));
		assertEquals(nodeResponse3.getDisplayName(), fileName);

		String query = "query($uuid: String){node(uuid: $uuid){ displayName }}";
		JsonObject variables = new JsonObject().put("uuid", nodeResponse1.getUuid());
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, new GraphQLRequest().setQuery(query).setVariables(variables)));
		assertEquals(response.getData().getJsonObject("node").getString("displayName"), fileName);
	}

	/**
	 * Svg images should not be transformed, since ImageIO can't read svg images.
	 */
	@Test
	public void testSvgTransformation() throws Exception {
		String fileName = "laptop-2.svg";
		InputStream ins = getClass().getResourceAsStream("/pictures/laptop-2.svg");
		byte[] inputBytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(inputBytes);
		String parentUuid = tx(() -> folder("2015").getUuid());

		tx(() -> group().addRole(roles().get("admin")));

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en").setParentNodeUuid(parentUuid).setSchemaName("binary_content");
		NodeResponse nodeResponse1 = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", nodeResponse1.getVersion(), "binary",
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), fileName,
			"image/svg"));

		MeshBinaryResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", "binary",
			new ImageManipulationParametersImpl().setWidth(100)));

		byte[] downloadBytes = IOUtils.toByteArray(download.getStream());
		download.close();

		assertThat(downloadBytes).containsExactly(inputBytes);
	}

	@Override
	public void testCreateNodeWithField() {
		// TODO Auto-generated method stub
	}

	@Override
	public void testCreateNodeWithNoField() {
		// TODO Auto-generated method stub
	}

	@Override
	public NodeResponse createNodeWithField() {
		String uuid = tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(() -> folder("2015").getGraphFieldContainer("en").getVersion());
		return call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"));
	}

}
