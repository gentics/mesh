package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeEndpointBinaryFieldTest extends AbstractMeshTest {

	@Before
	public void setupPerm() {
		grantAdminRole();
	}

	@Test
	public void testDownloadBinaryFieldWithReadPublishPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		// Only grant read_published perm
		try (Tx tx = tx()) {
			role().revokePermissions(node, READ_PERM);
			role().grantPermissions(node, READ_PUBLISHED_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 1. Upload some binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
			call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
			// 2. Download the data using the REST API
			MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary",
				new VersioningParametersImpl().setVersion("published")));
			assertEquals(binaryLen, IOUtils.toByteArray(response.getStream()).length);
			response.close();
		}
	}

	@Test
	public void testDownloadBinaryFieldDraftWithNoReadPublishPerm() throws IOException {
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		// Only grant read_published perm
		try (Tx tx = tx()) {
			role().revokePermissions(node, READ_PERM);
			role().grantPermissions(node, READ_PUBLISHED_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 1. Upload some binary data - This will update the draft
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 2. Download the data using the REST API
			call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary", new VersioningParametersImpl().setVersion("draft")),
				FORBIDDEN, "error_missing_perm", node.getUuid(), READ_PUBLISHED_PERM.getRestPerm().getName());

		}
	}

	/**
	 * Assert that a node can not be created if the binary field has not yet been created.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateNodeWithBinaryField() throws IOException {

		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaRequest = JsonUtil.readValue(tx(() -> schemaContainer("content").getLatestVersion().getJson()),
			SchemaUpdateRequest.class);
		schemaRequest.getFields().add(FieldUtil.createBinaryFieldSchema("binary"));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaRequest));
		}, COMPLETED, 1);

		SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
		schemaReference.setName("content");

		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.getFields().put("binary", new BinaryFieldImpl());
		request.setSchema(schemaReference);
		request.setParentNodeUuid(tx(() -> folder("news").getUuid()));
		call(() -> client().createNode(PROJECT_NAME, request));

		// Assert that the request fails when field has been specified.
		request.getFields().put("slug", FieldUtil.createStringField("new-page2.html"));
		request.getFields().put("binary", new BinaryFieldImpl().setDominantColor("#2E2EFE"));
		call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", "binary");

	}

	@Test
	public void testCreateNodeWithBinarySha512sum() {

		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaRequest = JsonUtil.readValue(tx(() -> schemaContainer("content").getLatestVersion().getJson()),
			SchemaUpdateRequest.class);
		schemaRequest.getFields().add(FieldUtil.createBinaryFieldSchema("binary"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaRequest));
		}, COMPLETED, 1);

		SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
		schemaReference.setName("content");

		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.getFields().put("binary", new BinaryFieldImpl().setSha512sum("someValue"));
		request.setSchema(schemaReference);
		request.setParentNodeUuid(tx(() -> folder("news").getUuid()));
		call(() -> client().createNode(PROJECT_NAME, request), BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", "binary");

	}

	/**
	 * Assert that it is possible to create a node which contains the binary field sha512sum. This node should reference the existing binary field binary.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateNodeWithBinaryFieldInfo() throws IOException {

		// Setup schema
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaUpdateRequest schemaRequest = JsonUtil.readValue(tx(() -> schemaContainer("content").getLatestVersion().getJson()),
			SchemaUpdateRequest.class);
		schemaRequest.getFields().add(FieldUtil.createBinaryFieldSchema("binary"));

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaRequest));
		}, COMPLETED, 1);

		// Upload some binary data
		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();
		NodeResponse response = call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
		String binaryUuid = response.getFields().getBinaryField("binary").getBinaryUuid();
		assertNotNull(binaryUuid);

		// Create Node
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.getFields().put("binary",
			new BinaryFieldImpl().setSha512sum(response.getFields().getBinaryField("binary").getSha512sum()).setDominantColor("#2E2EFE"));
		request.setSchemaName("content");
		request.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		NodeResponse createResponse = call(() -> client().createNode(PROJECT_NAME, request));
		assertEquals("The binary uuid should match up", binaryUuid, createResponse.getFields().getBinaryField("binary").getBinaryUuid());

		// Now delete the original node which provided the binary data
		call(() -> client().deleteNode(PROJECT_NAME, tx(() -> node.getUuid()), new DeleteParametersImpl().setRecursive(true)));

		// Assert that deletion did not affect the created node
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, createResponse.getUuid()));
		assertNotNull("The binary should still be attached to the node.", response.getFields().getBinaryField("binary").getBinaryUuid());

	}

	@Test
	public void testDownloadBinaryField() throws IOException {

		String contentType = "application/octet-stream";
		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();

		try (Tx tx = tx()) {
			// 1. Upload some binary data
			call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));

			// 2. Download the data using the REST API
			MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary"));
			assertEquals(binaryLen, IOUtils.toByteArray(response.getStream()).length);
			response.close();
		}
	}

	@Test
	public void testDownloadBinaryFieldRange() throws IOException {

		String contentType = "plain/text";
		String data = "Hello World!";

		int binaryLen = 8000;
		String fileName = "somefile.dat";
		Node node = prepareSchema();
		VersionNumber version = tx(() -> node.getGraphFieldContainer("en").getVersion());
		String uuid = tx(() -> node.getUuid());

		// 1. Upload some binary data
		Buffer buffer = Buffer.buffer(data);

		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.getBytes());
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, "en", version.toString(), "binary", stream, buffer.length(), fileName, contentType));

		MeshBinaryResponse response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary", 0, 4));
		String decoded = new String(IOUtils.toByteArray(response.getStream()));
		assertEquals("Hello", decoded);
		response.close();

		response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary", 6, 10));
		decoded = new String(IOUtils.toByteArray(response.getStream()));
		assertEquals("World", decoded);
		response.close();

		response = call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary", 0, 4));
		decoded = new String(IOUtils.toByteArray(response.getStream()));
		assertEquals("Hello", decoded);
		response.close();
	}

	/**
	 * Test downloading an image which already has a preconfigured focal point.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDownloadBinaryFieldWithPresetFocalPoint() throws IOException {

		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		InputStream ins = getClass().getResourceAsStream("/pictures/android-gps.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		// Create the node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setSchemaName("binary_content");
		NodeResponse node = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		// Upload the image
		NodeResponse node2 = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));

		// Update the stored focalpoint
		NodeUpdateRequest nodeUpdateRequest = node2.toRequest();
		BinaryField field = nodeUpdateRequest.getFields().getBinaryField("binary");
		field.setFocalPoint(0.4f, 0.2f);
		nodeUpdateRequest.getFields().put("binary", field);
		call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest));

		// Download the data using the REST API
		call(() -> client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", "binary",
			new ImageManipulationParametersImpl().setCropMode(CropMode.FOCALPOINT).setWidth(200).setHeight(300)));

	}

	@Test
	public void testUploadImagesConcurrently() throws IOException {
		String parentUuid;
		try (Tx tx = tx()) {
			Node node = folder("2015");
			parentUuid = node.getUuid();
			tx.success();
		}

		// Create schema with 2 binary fields
		SchemaCreateRequest schemaRequest = new SchemaCreateRequest().setName("imageSchema").setFields(
			Arrays.asList(new BinaryFieldSchemaImpl().setName("image1"), new BinaryFieldSchemaImpl().setName("image2").setRequired(true)));

		SchemaResponse schema = call(() -> client().createSchema(schemaRequest));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// Create node of that new schema
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest().setSchema(schema.toReference()).setParentNodeUuid(parentUuid).setLanguage("en");

		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		// Load the image from the file system
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		Buffer buffer = Buffer.buffer(IOUtils.toByteArray(ins));
		String blumeSum = "0b8f63eaa9893d994572a14a012c886d4b6b7b32f79df820f7aed201b374c89cf9d40f79345d5d76662ea733b23ed46dbaa243368627cbfe91a26c6452b88a29";

		io.reactivex.functions.Function<String, ObservableSource<NodeResponse>> uploadBinary = (fieldName) -> client()
			.updateNodeBinaryField(PROJECT_NAME, nodeResponse.getUuid(), nodeResponse.getLanguage(), nodeResponse.getVersion(), fieldName, new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
				"blume.jpg", "image/jpeg")
			.toObservable().doOnSubscribe((e) -> System.out.println("Requesting " + fieldName));

		Observable<String> imageFields = Observable.just("image1", "image2");

		// Upload 2 images at once
		// This should work since we can update the same node at the same time if it affects different fields
		imageFields.flatMap(uploadBinary).ignoreElements().blockingAwait();

		// Download them again and make sure they are the same image
		io.reactivex.functions.Function<String, ObservableSource<MeshBinaryResponse>> downloadBinary = (fieldName) -> client()
			.downloadBinaryField(PROJECT_NAME, nodeResponse.getUuid(), nodeResponse.getLanguage(), fieldName).toObservable();

		Consumer<String> assertSum = (sum) -> assertEquals("Checksum did not match", blumeSum, sum);

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeResponse.getUuid()));
		System.out.println(response.toJson());
		assertEquals("image/jpeg", response.getFields().getBinaryField("image1").getMimeType());
		assertEquals("image/jpeg", response.getFields().getBinaryField("image2").getMimeType());
		assertEquals("#737042", response.getFields().getBinaryField("image1").getDominantColor());
		assertEquals("#737042", response.getFields().getBinaryField("image2").getDominantColor());

		imageFields.flatMap(downloadBinary)
			.map(responseBody -> {
				Buffer body = Buffer.buffer(IOUtils.toByteArray(responseBody.getStream()));
				responseBody.close();
				return body;
			})
			.map(FileUtils::hash)
			.map(e -> e.blockingGet())
			.map(e -> assertSum)
			.ignoreElements().blockingAwait();
	}

	private Node prepareSchema() throws IOException {
		Node node = folder("news");

		try (Tx tx = tx()) {
			prepareSchema(node, "", "binary");
			tx.success();
		}
		return node;

	}
}
