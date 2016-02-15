package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;

public class AddFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setCustomMigrationScript("1234");
		assertEquals("1234", change.getMigrationScript());

		change.setFieldName("fieldName");
		assertEquals("fieldName", change.getFieldName());

		change.setRestProperty("key1", "value1");
		change.setRestProperty("key2", "value2");

		assertEquals("value1", change.getRestProperty("key1"));

	}

	@Test
	@Override
	public void testApply() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("name");
		change.setType("html");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("name");
	}

	@Test
	public void testApplyStringField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("stringField");
		change.setType("string");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("stringField");
		assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
	}

	@Test
	public void testApplyNodeField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("nodeField");
		change.setType("node");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("nodeField");
		assertTrue("The created field was not of the type node field." + updatedSchema.getField("nodeField").getClass(), updatedSchema.getField("nodeField") instanceof NodeFieldSchema);
	}

	@Test
	public void testApplyMicronodeField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("micronodeField");
		change.setType("micronode");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("micronodeField");
		assertTrue("The created field was not of the type micronode field.", updatedSchema.getField("micronodeField") instanceof MicronodeFieldSchema);
	}

	@Test
	public void testApplyDateField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("dateField");
		change.setType("date");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("dateField");
		assertTrue("The created field was not of the type date field.", updatedSchema.getField("dateField") instanceof DateFieldSchema);
	}

	@Test
	public void testApplyNumberField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("numberField");
		change.setType("number");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("numberField");
		assertTrue("The created field was not of the type number field.", updatedSchema.getField("numberField") instanceof NumberFieldSchema);
	}

	@Test
	public void testApplyBinaryField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("binaryField");
		change.setType("binary");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("binaryField");
		assertTrue("The created field was not of the type binary field.", updatedSchema.getField("binaryField") instanceof BinaryFieldSchema);
	}

	@Test
	public void testApplyListField() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("listField");
		change.setType("list");
		change.setListType("html");
		container.setSchema(schema);
		container.setNextChange(change);
		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertThat(updatedSchema).hasField("listField");
		assertTrue("The created field was not of the type binary field.", updatedSchema.getField("listField") instanceof ListFieldSchema);
		ListFieldSchema list = (ListFieldSchema) updatedSchema.getField("listField");
		assertEquals("The list type was incorrect.", "html", list.getListType());
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		SchemaChangeModel model = new SchemaChangeModel();
		model.setOperation(ADDFIELD);
		model.setMigrationScript("custom");
		model.setProperty(SchemaChangeModel.FIELD_NAME_KEY, "testField");
		model.setProperty(SchemaChangeModel.TYPE_KEY, "html");

		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.updateFromRest(model);
		assertEquals(change.getType(), model.getProperties().get(SchemaChangeModel.TYPE_KEY));
		assertEquals(change.getFieldName(), model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("name");
		change.setType("html");
		change.setRestProperty("someProperty", "test");

		SchemaChangeModel model = change.transformToRest();
		assertEquals(change.getUuid(), model.getUuid());
		assertEquals(change.getType(), model.getProperty(SchemaChangeModel.TYPE_KEY));
		assertEquals(change.getFieldName(), model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		assertEquals("The generic rest property from the change should have been set for the rest model.", "test",
				change.getRestProperty("someProperty"));
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		assertNull("Add field changes have no auto migation script.", change.getAutoMigrationScript());

		assertNull("Intitially no migration script should be set.", change.getMigrationScript());
		change.setCustomMigrationScript("test");
		assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
	}
}
