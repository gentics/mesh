package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.FILL_BASIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.node.TransformationResult;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.image.ImageInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.FileUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = false)
public class BinaryFieldTest extends AbstractFieldTest<BinaryFieldSchema> {

	private static final String BINARY_FIELD = "binaryField";

	private static final Base64.Decoder BASE64 = Base64.getDecoder();

	@Override
	protected BinaryFieldSchema createFieldSchema(boolean isRequired) {
		BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
		binaryFieldSchema.setName(BINARY_FIELD);
		binaryFieldSchema.setAllowedMimeTypes("image/jpg", "text/plain");
		binaryFieldSchema.setRequired(isRequired);
		return binaryFieldSchema;
	}

	@Test
	public void testBinaryFieldBase64() {
		try (Tx tx = tx()) {
			String input = "";
			while (input.length() < 32 * 1024) {
				input = input.concat("Hallo");
			}
			HibBinary binary = tx.binaries().create("hashsum", 1L).runInExistingTx(tx);
			mesh().binaryStorage().store(Flowable.just(Buffer.buffer(input)), binary.getUuid()).blockingAwait();
			String base64 = tx.binaryDao().getBase64ContentSync(binary);
			assertEquals(input.toString(), new String(BASE64.decode(base64)));
		}
	}

	@Test
	public void testBinaryFieldBase641Char() {
		try (Tx tx = tx()) {
			String input = " ";
			HibBinary binary = tx.binaries().create("hashsum", 1L).runInExistingTx(tx);
			mesh().binaryStorage().store(Flowable.just(Buffer.buffer(input)), binary.getUuid()).blockingAwait();
			String base64 = tx.binaryDao().getBase64ContentSync(binary);
			assertEquals(input.toString(), new String(BASE64.decode(base64)));
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		String hash = "6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc";
		HibNode node = folder("2015");
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.contentDao();
			// Update the schema and add a binary field
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();

			schema.addField(createFieldSchema(true));
			node.getSchemaContainer().getLatestVersion().setSchema(schema);
			NodeGraphFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibBinary binary = tx.binaries().create(hash, 10L).runInExistingTx(tx);
			BinaryGraphField field = container.createBinary(BINARY_FIELD, binary);
			field.setMimeType("image/jpg");
			binary.setImageHeight(200);
			binary.setImageWidth(300);
			tx.success();
		}

		try (Tx tx = tx()) {
			String json = getJson(node);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			BinaryField deserializedNodeField = response.getFields().getBinaryField(BINARY_FIELD);
			assertNotNull(deserializedNodeField);
			assertEquals(hash, deserializedNodeField.getSha512sum());
			assertEquals(200, deserializedNodeField.getHeight().intValue());
			assertEquals(300, deserializedNodeField.getWidth().intValue());
		}
	}

	@Test
	@Override
	public void testFieldUpdate() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibBinary binary = tx.binaries().create(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc",
				0L).runInExistingTx(tx);
			BinaryGraphField field = container.createBinary(BINARY_FIELD, binary);
			field.getBinary().setSize(220);
			assertNotNull(field);
			assertEquals(BINARY_FIELD, field.getFieldKey());

			field.setFileName("blume.jpg");
			field.setMimeType("image/jpg");
			field.setImageDominantColor("#22A7F0");
			field.getBinary().setImageHeight(133);
			field.getBinary().setImageWidth(7);

			BinaryGraphField loadedField = container.getBinary(BINARY_FIELD);
			HibBinary loadedBinary = loadedField.getBinary();
			assertNotNull("The previously created field could not be found.", loadedField);
			assertEquals(220, loadedBinary.getSize());

			assertEquals("blume.jpg", loadedField.getFileName());
			assertEquals("image/jpg", loadedField.getMimeType());
			assertEquals("#22A7F0", loadedField.getImageDominantColor());
			assertEquals(133, loadedField.getBinary().getImageHeight().intValue());
			assertEquals(7, loadedField.getBinary().getImageWidth().intValue());
			assertEquals(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc",
				loadedBinary.getSHA512Sum());
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			HibBinary binary = tx.binaries().create(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc",
				0L).runInExistingTx(tx);
			BinaryGraphField field = container.createBinary(BINARY_FIELD, binary);
			field.getBinary().setSize(220);
			assertNotNull(field);
			assertEquals(BINARY_FIELD, field.getFieldKey());

			field.setFileName("blume.jpg");
			field.setMimeType("image/jpg");
			field.setImageDominantColor("#22A7F0");
			field.getBinary().setImageHeight(133);
			field.getBinary().setImageWidth(7);

			NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			field.cloneTo(otherContainer);

			BinaryGraphField clonedField = otherContainer.getBinary(BINARY_FIELD);
			assertThat(clonedField).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(field, "outV", "id", "uuid", "element");
			assertThat(clonedField.getBinary()).as("referenced binary of cloned field").isNotNull().isEqualToComparingFieldByField(field.getBinary());
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

			HibBinary binary = tx.binaries().create(UUIDUtil.randomUUID(), 1L).runInExistingTx(tx);
			BinaryGraphField fieldA = container.createBinary("fieldA", binary);
			BinaryGraphField fieldB = container.createBinary("fieldB", binary);
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.setFileName("someText");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.setFileName("someText");
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibBinary binary = tx.binaries().create(UUIDUtil.randomUUID(), 0L).runInExistingTx(tx);
			BinaryGraphField fieldA = container.createBinary(BINARY_FIELD, binary);
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			HibBinary binary = tx.binaries().create("hashsum", 1L).runInExistingTx(tx);
			BinaryGraphField fieldA = container.createBinary("fieldA", binary);

			// graph empty - rest empty
			assertTrue("The field should be equal to the html rest field since both fields have no value.", fieldA.equals(new BinaryFieldImpl()));

			// graph set - rest set - same value - different type
			fieldA.setFileName("someText");
			assertFalse("The field should not be equal to a string rest field. Even if it has the same value", fieldA.equals(new StringFieldImpl()
				.setString("someText")));
			// graph set - rest set - different value
			assertFalse("The field should not be equal to the rest field since the rest field has a different value.", fieldA.equals(
				new BinaryFieldImpl().setFileName("blub")));

			// graph set - rest set - same value
			assertTrue("The binary field filename value should be equal to a rest field with the same value", fieldA.equals(new BinaryFieldImpl()
				.setFileName("someText")));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(BINARY_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(BINARY_FIELD, FETCH, false);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(BINARY_FIELD, FETCH, FILL_BASIC, (node) -> {
				updateContainer(ac, node, BINARY_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(BINARY_FIELD, FETCH, FILL_BASIC, (container) -> {
				updateContainer(ac, container, BINARY_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(BINARY_FIELD, FILL_BASIC, (container) -> {
				BinaryField field = new BinaryFieldImpl();
				field.setFileName("someFile.txt");
				updateContainer(ac, container, BINARY_FIELD, field);
			}, (container) -> {
				BinaryGraphField field = container.getBinary(BINARY_FIELD);
				assertNotNull("The graph field {" + BINARY_FIELD + "} could not be found.", field);
				assertEquals("The html of the field was not updated.", "someFile.txt", field.getFileName());
			});
		}
	}

	/**
	 * Verifies that the buffer stream of a source can be handled in parallel for hashing and image prop extraction.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMultiStreamHandling() throws IOException {
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Flowable<Buffer> obs = Flowable.just(Buffer.buffer(bytes)).publish().autoConnect(2);
		File file = new File("target", "file" + System.currentTimeMillis());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.write(bytes, fos);
			fos.flush();
		}

		Single<ImageInfo> info = mesh().imageManipulator().readImageInfo(file.getAbsolutePath());
		// Two obs handler
		Single<String> hash = FileUtils.hash(obs);
		Single<String> store = mesh().binaryStorage().store(obs, "bogus").toSingleDefault("null");

		TransformationResult result = Single.zip(hash, info, store, (hashV, infoV, storeV) -> {
			return new TransformationResult(hashV, 0, infoV, "blume.jpg");
		}).blockingGet();

		assertNotNull(result.getHash());
		assertEquals(1376, result.getImageInfo().getHeight().intValue());
		assertEquals(1160, result.getImageInfo().getWidth().intValue());
	}
}
