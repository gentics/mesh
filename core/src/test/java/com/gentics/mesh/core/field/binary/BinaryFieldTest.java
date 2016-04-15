package com.gentics.mesh.core.field.binary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class BinaryFieldTest extends AbstractFieldTest {

	private static final String BINARY_FIELD = "binaryField";

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");

		// Update the schema and add a binary field
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		BinaryFieldSchema binaryFieldSchema = new BinaryFieldSchemaImpl();
		binaryFieldSchema.setName("binaryField");
		binaryFieldSchema.setAllowedMimeTypes("image/jpg", "text/plain");
		schema.addField(binaryFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
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
	public void testEqualsNull() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BinaryGraphField fieldA = container.createBinary(BINARY_FIELD);
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));
	}

	@Test
	@Override
	public void testFieldUpdate() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);

		BinaryGraphField field = container.createBinary("testBinaryField");
		assertNotNull(field);
		assertEquals("testBinaryField", field.getFieldKey());

		field.setFileName("blume.jpg");
		field.setMimeType("image/jpg");
		field.setFileSize(220);
		field.setImageDPI(200);
		field.setImageHeight(133);
		field.setImageWidth(7);
		field.setSHA512Sum(
				"6a793cf1c7f6ef022ba9fff65ed43ddac9fb9c2131ffc4eaa3f49212244c0d4191ae5877b03bd50fd137bd9e5a16799da4a1f2846f0b26e3d956c4d8423004cc");
		System.out.println(field.getSegmentedPath());

		BinaryGraphField loadedField = container.getBinary("testBinaryField");
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
		assertEquals("testBinaryField", field.getFieldKey());

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

	@Override
	public void testEquals() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEqualsRestField() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testRemoveFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected FieldSchema createFieldSchema(boolean isRequired) {
		// TODO Auto-generated method stub
		return null;
	}
}
