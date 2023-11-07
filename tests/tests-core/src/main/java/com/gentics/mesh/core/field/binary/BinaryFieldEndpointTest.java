package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.VersionNumber;

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

	private void setSchema(boolean isRequired) throws IOException {
		try (Tx tx = tx()) {
			// add non restricted string field
			BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
			binaryFieldSchema.setName(FIELD_NAME);
			binaryFieldSchema.setLabel("Some label");
			binaryFieldSchema.setRequired(isRequired);
			prepareTypedSchema(schemaContainer("folder"), List.of(binaryFieldSchema), Optional.empty());
			prepareTypedSchema(folder("2015"), binaryFieldSchema, true);
			tx.success();
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
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });
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
			VersionNumber version = tx(() -> tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
				"application/binary"));
		}
		NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().setVersion("draft")));
		assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
		String oldVersion = firstResponse.getVersion();
		BinaryField binaryField = firstResponse.getFields().getBinaryField(FIELD_NAME);

		// 2. Update the node using the loaded binary field data
		NodeResponse secondResponse = updateNode(FIELD_NAME, binaryField);
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getVersion()).as("New version number should not be generated.").isEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		String filename = "filename.txt";
		Buffer buffer = TestUtils.randomBuffer(1000);

		// 1. Upload a binary field
		String uuid = tx(() -> folder("2015").getUuid());
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });

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
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getBinary(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getBinary(FIELD_NAME)).isNotNull();
			String oldFilename = latest.getPreviousVersion().getBinary(FIELD_NAME).getFileName();
			assertThat(oldFilename).as("Old version filename should match the intitial version filename").isEqualTo(filename);
		}
		// 3. Set the field to null one more time and assert that no new version was created
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse
			.getVersion());
	}

	@Test
	public void testUpdateDelete() throws IOException {
		// 1. Upload a binary field
		NodeResponse response = createNodeWithField();

		// Clear the local binary storage directory to simulate a storage inconsistency
		FileUtils.deleteDirectory(new File(options().getUploadOptions().getDirectory()));

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
			VersionNumber version = tx(() -> tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion());
			call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
				"application/binary"));
		}

		NodeResponse firstResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, folder("2015").getUuid(), new VersioningParametersImpl().setVersion("draft")));
		assertEquals("filename.txt", firstResponse.getFields().getBinaryField(FIELD_NAME).getFileName());
		String oldVersion = firstResponse.getVersion();

		// 2. Set the field to empty - Node should not be updated since nothing changes
		NodeResponse secondResponse = updateNode(FIELD_NAME, new BinaryFieldImpl());
		assertThat(secondResponse.getFields().getBinaryField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
	}

	@Test
	public void testUpdateSetEmptyFilename() {
		String uuid = tx(() -> folder("2015").getUuid());
		// 1. Upload a binary field
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });
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
		byte[] bytes = getBinary("/pictures/blume.jpg");
		NodeResponse nodeResponse1 = createBinaryNode();

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", nodeResponse1.getVersion(), "binary",
			new ByteArrayInputStream(bytes), bytes.length, fileName,
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
		byte[] inputBytes = getBinary("/pictures/laptop-2.svg");
		NodeResponse nodeResponse1 = createBinaryNode();

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", nodeResponse1.getVersion(), "binary",
			new ByteArrayInputStream(inputBytes), inputBytes.length, fileName,
			"image/svg"));

		MeshBinaryResponse download = call(() -> client().downloadBinaryField(PROJECT_NAME, nodeResponse1.getUuid(), "en", "binary",
			new ImageManipulationParametersImpl().setWidth(100)));

		byte[] downloadBytes = IOUtils.toByteArray(download.getStream());
		download.close();

		assertThat(downloadBytes).containsExactly(inputBytes);
	}

	/**
	 * Test for https://github.com/gentics/mesh/issues/669
	 */
	@Test
	public void testSimilarManipulationParameters() throws IOException {
		String fileName = "blume.jpg";
		byte[] bytes = getBinary("/pictures/blume.jpg");

		NodeResponse nodeResponse = createBinaryNode();

		call(() -> client().updateNodeBinaryField(PROJECT_NAME, nodeResponse.getUuid(), "en", nodeResponse.getVersion(), "binary",
			new ByteArrayInputStream(bytes), bytes.length, fileName,"image/jpg"));

		String hash = hashBinary(client().downloadBinaryField(
			PROJECT_NAME, nodeResponse.getUuid(), "en", "binary",
			new ImageManipulationParametersImpl()
				.setWidth(300).setHeight(400)
				.setFocalPoint(0.46f, 0.35f)
				.setCropMode(CropMode.FOCALPOINT)
				.setFocalPointZoom(2f)
		));

		String hash2 = hashBinary(client().downloadBinaryField(
			PROJECT_NAME, nodeResponse.getUuid(), "en", "binary",
			new ImageManipulationParametersImpl()
				.setWidth(300).setHeight(400)
				.setFocalPoint(0.46f, 0.35f)
				.setCropMode(CropMode.FOCALPOINT)
		));

		assertNotEquals("Downloaded binary must be different", hash, hash2);
	}

	private String hashBinary(MeshRequest<MeshBinaryResponse> downloadBinaryField) throws IOException {
		return DigestUtils.md5Hex(downloadBinaryField.blockingGet().getStream());
	}

	private NodeResponse createBinaryNode() {
		String parentUuid = tx(() -> folder("2015").getUuid());

		grantAdmin();

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en").setParentNodeUuid(parentUuid).setSchemaName("binary_content");
		return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
	}

	private byte[] getBinary(String name) throws IOException {
		InputStream ins = getClass().getResourceAsStream(name);
		return IOUtils.toByteArray(ins);
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
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });
		return call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"));
	}

	/**
	 * Tries to download a binary that was already deleted in the filesystem.
	 */
	@Test
	public void downloadDeletedBinary() throws IOException {
		String fileName = "blume.jpg";
		byte[] bytes = getBinary("/pictures/blume.jpg");

		NodeResponse createResponse = createBinaryNode();

		NodeResponse updatedResponse = client().updateNodeBinaryField(PROJECT_NAME, createResponse.getUuid(), "en", createResponse.getVersion(), "binary",
			new ByteArrayInputStream(bytes), bytes.length, fileName, "image/jpg").blockingGet();

		// Clear the local binary storage directory to simulate a storage inconsistency
		FileUtils.deleteDirectory(new File(options().getUploadOptions().getDirectory()));

		call(() -> client().downloadBinaryField(PROJECT_NAME, updatedResponse.getUuid(), updatedResponse.getLanguage(), "binary"),
			NOT_FOUND, "node_error_binary_data_not_found");

		call(() -> client().downloadBinaryField(PROJECT_NAME,
			updatedResponse.getUuid(),
			updatedResponse.getLanguage(),
			"binary",
			new ImageManipulationParametersImpl().setWidth(200)
		), NOT_FOUND, "node_error_binary_data_not_found");

		call(() -> client().transformNodeBinaryField(PROJECT_NAME, updatedResponse.getUuid(), updatedResponse.getLanguage(), updatedResponse.getVersion(),
			"binary", new ImageManipulationParametersImpl().setWidth(250)),
			NOT_FOUND, "node_error_binary_data_not_found");
	}

	@Test
	public void testUploadEmptyFile() {
		NodeResponse binaryNode = createBinaryNode();

		InputStream emptyStream = new InputStream() {
			@Override
			public int read() throws IOException {
				return -1;
			}
		};

		try {
			client().updateNodeBinaryField(
				PROJECT_NAME,
				binaryNode.getUuid(),
				binaryNode.getLanguage(),
				binaryNode.getVersion(),
				"binary",
				emptyStream,
				0,
				"emptyFile",
				"application/binary"
			).blockingAwait();
			fail("Empty file upload should not pass");
		} catch (Exception e) {
			assertTrue(e.getMessage().indexOf("Error:400 in POST") > -1);
		}
	}

	/**
	 * Test uploading binary data without publishing
	 */
	@Test
	public void testUploadAndDoNotPublish() {
		String uuid = tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });
		NodeResponse updated = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary"));

		assertThat(updated).hasVersion("1.1").hasLanguageVariant("en", true);
	}

	/**
	 * Test uploading binary data and publishing
	 */
	@Test
	public void testUploadAndPublish() {
		String uuid = tx(() -> folder("2015").getUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);
		VersionNumber version = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });
		NodeResponse updated = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), FIELD_NAME,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "filename.txt",
			"application/binary", true));

		assertThat(updated).hasVersion("2.0").hasLanguageVariant("en", true);
	}
}
