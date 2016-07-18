package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.binary.BinaryFieldTestHelper.FILL_BASIC;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class BinaryFieldTest extends AbstractFieldTest<BinaryFieldSchema> {

	private static final String BINARY_FIELD = "binaryField";

	@Override
	protected BinaryFieldSchema createFieldSchema(boolean isRequired) {
		BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
		binaryFieldSchema.setName(BINARY_FIELD);
		binaryFieldSchema.setAllowedMimeTypes("image/jpg", "text/plain");
		binaryFieldSchema.setRequired(isRequired);
		return binaryFieldSchema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");

		// Update the schema and add a binary field
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();

		schema.addField(createFieldSchema(true));
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
		BinaryGraphField field = container.createBinary(BINARY_FIELD);
		field.setMimeType("image/jpg");
		field.setSHA512Sum(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc");
		field.setImageHeight(200);
		field.setImageWidth(300);

		String json = getJson(node);
		System.out.println(json);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		BinaryField deserializedNodeField = response.getFields().getBinaryField(BINARY_FIELD);
		assertNotNull(deserializedNodeField);
		assertEquals(200, deserializedNodeField.getHeight().intValue());
		assertEquals(300, deserializedNodeField.getWidth().intValue());

	}

	@Test
	@Override
	public void testFieldUpdate() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		BinaryGraphField field = container.createBinary(BINARY_FIELD);
		assertNotNull(field);
		assertEquals(BINARY_FIELD, field.getFieldKey());

		field.setFileName("blume.jpg");
		field.setMimeType("image/jpg");
		field.setFileSize(220);
		field.setImageDPI(200);
		field.setImageHeight(133);
		field.setImageWidth(7);
		field.setSHA512Sum(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc");
		System.out.println(field.getSegmentedPath());

		BinaryGraphField loadedField = container.getBinary(BINARY_FIELD);
		assertNotNull("The previously created field could not be found.", loadedField);
		assertEquals(220, loadedField.getFileSize());

		assertEquals("blume.jpg", loadedField.getFileName());
		assertEquals("image/jpg", loadedField.getMimeType());
		assertEquals(220, loadedField.getFileSize());
		assertEquals(200, loadedField.getImageDPI().intValue());
		assertEquals(133, loadedField.getImageHeight().intValue());
		assertEquals(7, loadedField.getImageWidth().intValue());
		assertEquals(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc",
				loadedField.getSHA512Sum());

	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		BinaryGraphField field = container.createBinary(BINARY_FIELD);
		assertNotNull(field);
		assertEquals(BINARY_FIELD, field.getFieldKey());

		field.setFileName("blume.jpg");
		field.setMimeType("image/jpg");
		field.setFileSize(220);
		field.setImageDPI(200);
		field.setImageHeight(133);
		field.setImageWidth(7);
		field.setSHA512Sum(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		field.cloneTo(otherContainer);

		BinaryGraphField clonedField = otherContainer.getBinary(BINARY_FIELD);
		assertThat(clonedField).as("cloned field").isNotNull().isEqualToComparingFieldByField(field);
	}

	@Test
	@Override
	public void testEquals() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BinaryGraphField fieldA = container.createBinary("fieldA");
		BinaryGraphField fieldB = container.createBinary("fieldB");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
		fieldA.setFileName("someText");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

		assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
		assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
		fieldB.setFileName("someText");
		assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
	}

	@Test
	@Override
	public void testEqualsNull() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BinaryGraphField fieldA = container.createBinary(BINARY_FIELD);
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BinaryGraphField fieldA = container.createBinary("fieldA");

		// graph empty - rest empty 
		assertTrue("The field should be equal to the html rest field since both fields have no value.", fieldA.equals(new BinaryFieldImpl()));

		// graph set - rest set - same value - different type
		fieldA.setFileName("someText");
		assertFalse("The field should not be equal to a string rest field. Even if it has the same value",
				fieldA.equals(new StringFieldImpl().setString("someText")));
		// graph set - rest set - different value
		assertFalse("The field should not be equal to the rest field since the rest field has a different value.",
				fieldA.equals(new BinaryFieldImpl().setFileName("blub")));

		// graph set - rest set - same value
		assertTrue("The binary field filename value should be equal to a rest field with the same value",
				fieldA.equals(new BinaryFieldImpl().setFileName("someText")));
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		invokeUpdateFromRestTestcase(BINARY_FIELD, FETCH, CREATE_EMPTY);
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		invokeUpdateFromRestNullOnCreateRequiredTestcase(BINARY_FIELD, FETCH, false);
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		InternalActionContext ac = getMockedInternalActionContext();
		invokeRemoveFieldViaNullTestcase(BINARY_FIELD, FETCH, FILL_BASIC, (node) -> {
			updateContainer(ac, node, BINARY_FIELD, null);
		});
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		InternalActionContext ac = getMockedInternalActionContext();
		invokeRemoveRequiredFieldViaNullTestcase(BINARY_FIELD, FETCH, FILL_BASIC, (container) -> {
			updateContainer(ac, container, BINARY_FIELD, null);
		});
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		InternalActionContext ac = getMockedInternalActionContext();
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
