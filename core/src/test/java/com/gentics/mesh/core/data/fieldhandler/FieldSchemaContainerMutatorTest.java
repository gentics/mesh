package com.gentics.mesh.core.data.fieldhandler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractEmptyDBTest;
import com.gentics.mesh.util.FieldUtil;

/**
 * Test for common mutator operations on a field containers.
 */
public class FieldSchemaContainerMutatorTest extends AbstractEmptyDBTest {

	@Autowired
	private FieldSchemaContainerMutator mutator;

	@Test
	public void testNullOperation() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		container.setSchema(schema);
		Schema updatedSchema = mutator.apply(container);
		assertNotNull(updatedSchema);
		assertEquals("No changes were specified. No modification should happen.", schema, updatedSchema);
	}

	@Test
	public void testNameDescription() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.setName("updated");
		container.setSchema(schema);
		container.setNextChange(change);

		Schema updatedSchema = mutator.apply(container);
		assertEquals("updated", updatedSchema.getName());

		change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.setDescription("text");
		container.setNextChange(change);
		updatedSchema = mutator.apply(container);
		assertEquals("text", updatedSchema.getDescription());
	}

	@Test
	public void testAddField() {
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
	public void testUpdateFieldLabel() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		Schema schema = new SchemaImpl("test");
		schema.addField(FieldUtil.createStringFieldSchema("name"));

		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.setFieldName("name");
		change.setLabel("updated");
		container.setSchema(schema);
		container.setNextChange(change);

		FieldSchemaContainer updatedSchema = mutator.apply(container);
		assertEquals("The field label was not updated by the mutator.", "updated", updatedSchema.getField("name").getLabel());
	}

	@Test
	public void testFieldOrderChange() {
		// 1. Create the schema container
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		Schema schema = new SchemaImpl();
		schema.addField(FieldUtil.createHtmlFieldSchema("first"));
		schema.addField(FieldUtil.createHtmlFieldSchema("second"));
		container.setSchema(schema);

		// 2. Create the schema update change
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.setOrder("second", "first");
		container.setNextChange(change);

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(container);
		assertNotNull("The updated schema was not generated.", updatedSchema);
		assertEquals("The updated schema should contain two fields.", 2, updatedSchema.getFields().size());
		assertEquals("The first field should now be the field with name \"second\".", "second", updatedSchema.getFields().get(0).getName());
		assertEquals("The second field should now be the field with the name \"first\".", "first", updatedSchema.getFields().get(1).getName());

	}

	@Test
	public void testRemoveField() {

		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		// 1. Create schema with field
		Schema schema = new SchemaImpl();
		schema.addField(FieldUtil.createStringFieldSchema("test"));

		// 2. Create remove field change
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName("test");

		container.setNextChange(change);
		container.setSchema(schema);

		// 3. Apply the change
		FieldSchemaContainer updatedSchema = mutator.apply(container);

		assertThat(updatedSchema).hasNoField("test");
	}

	//	@Test
	//	public void testRemoveNonExistingField() {
	//		Schema schema = new SchemaImpl();
	//		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
	//		change.setFieldName("test");
	//
	//		// 3. Apply the change
	//		FieldSchemaContainer updatedContainer = mutator.apply(schema, Arrays.asList(change));
	//
	//		fail("TODO define result");
	//	}

	@Test
	public void testAUpdateFields() {

		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		// 1. Create schema
		Schema schema = new SchemaImpl("testschema");

		BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
		binaryField.setName("binaryField");
		binaryField.setAllowedMimeTypes("oldTypes");
		binaryField.setRequired(true);
		schema.addField(binaryField);

		StringFieldSchema stringField = new StringFieldSchemaImpl();
		stringField.setAllowedValues("blub");
		stringField.setName("stringField");
		stringField.setRequired(true);
		schema.addField(stringField);

		NodeFieldSchema nodeField = new NodeFieldSchemaImpl();
		nodeField.setAllowedSchemas("blub");
		nodeField.setName("nodeField");
		nodeField.setRequired(true);
		schema.addField(nodeField);

		MicronodeFieldSchema micronodeField = new MicronodeFieldSchemaImpl();
		micronodeField.setAllowedMicroSchemas("blub");
		micronodeField.setName("micronodeField");
		micronodeField.setRequired(true);
		schema.addField(micronodeField);

		NumberFieldSchema numberField = new NumberFieldSchemaImpl();
		numberField.setName("numberField");
		numberField.setRequired(true);
		schema.addField(numberField);

		HtmlFieldSchema htmlField = new HtmlFieldSchemaImpl();
		htmlField.setName("htmlField");
		htmlField.setRequired(true);
		schema.addField(htmlField);

		BooleanFieldSchema booleanField = new BooleanFieldSchemaImpl();
		booleanField.setName("booleanField");
		booleanField.setRequired(true);
		schema.addField(booleanField);

		DateFieldSchema dateField = new DateFieldSchemaImpl();
		dateField.setName("dateField");
		dateField.setRequired(true);
		schema.addField(dateField);

		ListFieldSchema listField = new ListFieldSchemaImpl();
		listField.setName("listField");
		listField.setRequired(true);
		schema.addField(listField);

		container.setSchema(schema);

		// 2. Create schema field update change
		UpdateFieldChange binaryFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		binaryFieldUpdate.setFieldName("binaryField");
		binaryFieldUpdate.setFieldProperty("allowedMimeTypes", new String[] { "newTypes" });
		binaryFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		container.setNextChange(binaryFieldUpdate);

		UpdateFieldChange nodeFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		nodeFieldUpdate.setFieldName("nodeField");
		nodeFieldUpdate.setFieldProperty("allowedSchemas", new String[] { "schemaA", "schemaB" });
		nodeFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		binaryFieldUpdate.setNextChange(nodeFieldUpdate);

		UpdateFieldChange stringFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		stringFieldUpdate.setFieldProperty("allowedValues", new String[] { "valueA", "valueB" });
		stringFieldUpdate.setFieldName("stringField");
		stringFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		nodeFieldUpdate.setNextChange(stringFieldUpdate);

		UpdateFieldChange htmlFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		htmlFieldUpdate.setFieldName("htmlField");
		htmlFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		stringFieldUpdate.setNextChange(htmlFieldUpdate);

		UpdateFieldChange numberFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		numberFieldUpdate.setFieldName("numberField");
		numberFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		htmlFieldUpdate.setNextChange(numberFieldUpdate);

		UpdateFieldChange dateFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		dateFieldUpdate.setFieldName("dateField");
		dateFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		numberFieldUpdate.setNextChange(dateFieldUpdate);

		UpdateFieldChange booleanFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		booleanFieldUpdate.setFieldName("booleanField");
		booleanFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		dateFieldUpdate.setNextChange(booleanFieldUpdate);

		UpdateFieldChange micronodeFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		micronodeFieldUpdate.setFieldName("micronodeField");
		micronodeFieldUpdate.setFieldProperty(SchemaChangeModel.ALLOW_KEY, new String[] { "A", "B", "C" });
		micronodeFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		booleanFieldUpdate.setNextChange(micronodeFieldUpdate);

		UpdateFieldChange listFieldUpdate = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		listFieldUpdate.setFieldName("listField");
		listFieldUpdate.setFieldProperty(SchemaChangeModel.REQUIRED_KEY, false);
		micronodeFieldUpdate.setNextChange(listFieldUpdate);

		// 3. Apply the changes
		Schema updatedSchema = mutator.apply(container);

		// Binary 
		BinaryFieldSchema binaryFieldSchema = updatedSchema.getField("binaryField", BinaryFieldSchemaImpl.class);
		assertNotNull(binaryFieldSchema);
		assertArrayEquals(new String[] { "newTypes" }, binaryFieldSchema.getAllowedMimeTypes());
		assertFalse("The required flag should now be set to false.", binaryFieldSchema.isRequired());

		// Node
		NodeFieldSchema nodeFieldSchema = updatedSchema.getField("nodeField", NodeFieldSchemaImpl.class);
		assertNotNull(nodeFieldSchema);
		assertArrayEquals(new String[] { "schemaA", "schemaB" }, nodeFieldSchema.getAllowedSchemas());
		assertFalse("The required flag should now be set to false.", nodeFieldSchema.isRequired());

		// Microschema
		MicronodeFieldSchema micronodeFieldSchema = updatedSchema.getField("micronodeField", MicronodeFieldSchemaImpl.class);
		assertNotNull(micronodeFieldSchema);
		assertArrayEquals(new String[] { "A", "B", "C" }, micronodeFieldSchema.getAllowedMicroSchemas());
		assertFalse("The required flag should now be set to false.", micronodeFieldSchema.isRequired());

		// String
		StringFieldSchema stringFieldSchema = updatedSchema.getField("stringField", StringFieldSchemaImpl.class);
		assertNotNull(stringFieldSchema);
		assertArrayEquals(new String[] { "valueA", "valueB" }, stringFieldSchema.getAllowedValues());
		assertFalse("The required flag should now be set to false.", stringFieldSchema.isRequired());

		// Html
		HtmlFieldSchema htmlFieldSchema = updatedSchema.getField("htmlField", HtmlFieldSchemaImpl.class);
		assertNotNull(htmlFieldSchema);
		assertFalse("The required flag should now be set to false.", htmlFieldSchema.isRequired());

		// Boolean
		BooleanFieldSchema booleanFieldSchema = updatedSchema.getField("booleanField", BooleanFieldSchemaImpl.class);
		assertFalse("The required flag should now be set to false.", booleanFieldSchema.isRequired());

		// Date
		DateFieldSchema dateFieldSchema = updatedSchema.getField("dateField", DateFieldSchemaImpl.class);
		assertFalse("The required flag should now be set to false.", dateFieldSchema.isRequired());

		// Number
		NumberFieldSchema numberFieldSchema = updatedSchema.getField("numberField", NumberFieldSchemaImpl.class);
		assertFalse("The required flag should now be set to false.", numberFieldSchema.isRequired());

		// List
		ListFieldSchema listFieldSchema = updatedSchema.getField("listField", ListFieldSchemaImpl.class);
		assertFalse("The required flag should now be set to false.", listFieldSchema.isRequired());

	}

	@Test
	public void testChangeFieldTypeToHtml() {

		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		// 1. Create schema
		Schema schema = new SchemaImpl();
		schema.setName("testschema");

		StringFieldSchema stringField = new StringFieldSchemaImpl();
		stringField.setName("stringField");
		stringField.setRequired(true);
		schema.addField(stringField);

		FieldTypeChange fieldTypeUpdate = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		fieldTypeUpdate.setFieldName("stringField");
		fieldTypeUpdate.setFieldProperty("newType", "html");

		// 3. Apply the changes
		container.setNextChange(fieldTypeUpdate);
		container.setSchema(schema);
		for (FieldSchema field : container.getSchema().getFields()) {
			System.out.println(field.getName());
		}

		Schema updatedSchema = mutator.apply(container);
		assertNotNull(updatedSchema);
		assertEquals("html", updatedSchema.getField("stringField").getType());

	}

	@Test
	public void testChangeFieldTypeToList() {

		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		// 1. Create schema
		Schema schema = new SchemaImpl();
		schema.setName("testschema");

		StringFieldSchema stringField = new StringFieldSchemaImpl();
		stringField.setName("stringField");
		stringField.setRequired(true);
		stringField.setLabel("test123");
		schema.addField(stringField);

		FieldTypeChange fieldTypeUpdate = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		fieldTypeUpdate.setFieldName("stringField");
		fieldTypeUpdate.setFieldProperty("newType", "list");
		fieldTypeUpdate.setFieldProperty("listType", "html");

		container.setNextChange(fieldTypeUpdate);
		container.setSchema(schema);

		// 3. Apply the changes
		Schema updatedSchema = mutator.apply(container);
		assertNotNull(updatedSchema);
		ListFieldSchema fieldSchema = updatedSchema.getField("stringField", ListFieldSchemaImpl.class);
		assertEquals("list", fieldSchema.getType());
		assertEquals("html", fieldSchema.getListType());
		assertEquals("test123", fieldSchema.getLabel());

	}

	@Test
	public void testUpdateSchema() {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		// 1. Create schema
		Schema schema = new SchemaImpl();

		// 2. Create schema update change
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.setDisplayField("newDisplayField");
		change.setContainerFlag(true);
		change.setSegmentField("newSegmentField");

		container.setSchema(schema);
		container.setNextChange(change);

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(container);
		assertEquals("The display field name was not updated", "newDisplayField", updatedSchema.getDisplayField());
		assertEquals("The segment field name was not updated", "newSegmentField", updatedSchema.getSegmentField());
		assertTrue("The schema container flag was not updated", updatedSchema.isContainer());
	}

}
