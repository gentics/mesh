package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class AddFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = db().tx()) {
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setCustomMigrationScript("1234");
			assertEquals("1234", change.getMigrationScript());

			change.setFieldName("fieldName");
			assertEquals("fieldName", change.getFieldName());

			change.setRestProperty("key1", "value1");
			change.setRestProperty("key2", "value2");

			assertEquals("value1", change.getRestProperty("key1"));

		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("name");
			change.setType("html");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("name");
		}
	}

	@Test
	public void testApplayStringFieldAtEndPosition() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModel schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("firstField"));
			schema.addField(FieldUtil.createStringFieldSchema("secondField"));
			schema.addField(FieldUtil.createStringFieldSchema("thirdField"));

			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("stringField");
			change.setType("string");
			change.setInsertAfterPosition("thirdField");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertArrayEquals(new String[] { "firstField", "secondField", "thirdField", "stringField" },
					updatedSchema.getFields().stream().map(field -> field.getName()).toArray());
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyStringFieldAtPosition() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.addField(FieldUtil.createStringFieldSchema("firstField"));
			schema.addField(FieldUtil.createStringFieldSchema("secondField"));
			schema.addField(FieldUtil.createStringFieldSchema("thirdField"));

			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("stringField");
			change.setType("string");
			change.setInsertAfterPosition("firstField");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertArrayEquals(new String[] { "firstField", "stringField", "secondField", "thirdField" },
					updatedSchema.getFields().stream().map(field -> field.getName()).toArray());
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyStringField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("stringField");
			change.setType("string");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("stringField");
			assertTrue("The created field was not of the string string field.", updatedSchema.getField("stringField") instanceof StringFieldSchema);
		}
	}

	@Test
	public void testApplyNodeField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("nodeField");
			change.setType("node");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("nodeField");
			assertTrue("The created field was not of the type node field." + updatedSchema.getField("nodeField").getClass(),
					updatedSchema.getField("nodeField") instanceof NodeFieldSchema);
		}
	}

	@Test
	public void testApplyMicronodeField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("micronodeField");
			change.setType("micronode");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("micronodeField");
			assertTrue("The created field was not of the type micronode field.",
					updatedSchema.getField("micronodeField") instanceof MicronodeFieldSchema);
		}
	}

	@Test
	public void testApplyDateField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("dateField");
			change.setType("date");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("dateField");
			assertTrue("The created field was not of the type date field.", updatedSchema.getField("dateField") instanceof DateFieldSchema);
		}
	}

	@Test
	public void testApplyNumberField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("numberField");
			change.setType("number");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("numberField");
			assertTrue("The created field was not of the type number field.", updatedSchema.getField("numberField") instanceof NumberFieldSchema);
		}
	}

	@Test
	public void testApplyBinaryField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("binaryField");
			change.setType("binary");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("binaryField");
			assertTrue("The created field was not of the type binary field.", updatedSchema.getField("binaryField") instanceof BinaryFieldSchema);
		}
	}

	@Test
	public void testApplyListField() {
		try (Tx tx = db().tx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaModelImpl schema = new SchemaModelImpl();
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.setFieldName("listField");
			change.setType("list");
			change.setListType("html");
			version.setSchema(schema);
			version.setNextChange(change);
			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertThat(updatedSchema).hasField("listField");
			assertTrue("The created field was not of the type binary field.", updatedSchema.getField("listField") instanceof ListFieldSchema);
			ListFieldSchema list = (ListFieldSchema) updatedSchema.getField("listField");
			assertEquals("The list type was incorrect.", "html", list.getListType());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (Tx tx = db().tx()) {
			SchemaChangeModel model = SchemaChangeModel.createAddFieldChange("testField", "html", "test123");
			model.setMigrationScript("custom");

			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			change.updateFromRest(model);
			assertEquals(change.getType(), model.getProperties().get(SchemaChangeModel.TYPE_KEY));
			assertEquals(change.getFieldName(), model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals(change.getLabel(), model.getProperty(SchemaChangeModel.LABEL_KEY));
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = db().tx()) {
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
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		try (Tx tx = db().tx()) {
			AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
			assertNull("Add field changes have no auto migation script.", change.getAutoMigrationScript());

			assertNull("Intitially no migration script should be set.", change.getMigrationScript());
			change.setCustomMigrationScript("test");
			assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
		}
	}
}
