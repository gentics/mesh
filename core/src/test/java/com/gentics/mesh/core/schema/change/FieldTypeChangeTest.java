package com.gentics.mesh.core.schema.change;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class FieldTypeChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			FieldTypeChange change = tx.createVertex(FieldTypeChangeImpl.class);
			change.setFieldName("name");
			assertEquals("name", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.createVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.setName("testschema");

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setName("stringField");
			stringField.setRequired(true);
			schema.addField(stringField);

			FieldTypeChange fieldTypeUpdate = tx.createVertex(FieldTypeChangeImpl.class);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("html");

			// 3. Apply the changes
			version.setNextChange(fieldTypeUpdate);
			version.setSchema(schema);

			Schema updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			assertEquals("html", updatedSchema.getField("stringField").getType());
		}
	}

	@Test
	public void testChangeFieldTypeToList() {
		try (Tx tx = tx()) {
			SchemaContainerVersion version = tx.createVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.setName("testschema");

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setName("stringField");
			stringField.setRequired(true);
			stringField.setLabel("test123");
			schema.addField(stringField);

			FieldTypeChange fieldTypeUpdate = tx.createVertex(FieldTypeChangeImpl.class);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("list");
			fieldTypeUpdate.setListType("html");

			version.setNextChange(fieldTypeUpdate);
			version.setSchema(schema);

			// 3. Apply the changes
			Schema updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			ListFieldSchema fieldSchema = updatedSchema.getField("stringField", ListFieldSchemaImpl.class);
			assertEquals("list", fieldSchema.getType());
			assertEquals("html", fieldSchema.getListType());
			assertEquals("test123", fieldSchema.getLabel());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() throws IOException {
		try (Tx tx = tx()) {
			SchemaChangeModel model = SchemaChangeModel.createChangeFieldTypeChange("testField", "list");
			model.setMigrationScript("test");
			model.setProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
			FieldTypeChange change = tx.createVertex(FieldTypeChangeImpl.class);
			change.updateFromRest(model);

			assertEquals("test", change.getMigrationScript());
			assertEquals("testField", change.getFieldName());
			assertEquals("list", change.getType());
			assertEquals("html", change.getListType());
		}
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		try (Tx tx = tx()) {
			FieldTypeChange change = tx.createVertex(FieldTypeChangeImpl.class);
			assertNotNull("Field Type changes have a auto migation script.", change.getAutoMigrationScript());

			assertNotNull("Intitially the default migration script should be set.", change.getMigrationScript());
			change.setCustomMigrationScript("test");
			assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			FieldTypeChange change = tx.createVertex(FieldTypeChangeImpl.class);
			change.setFieldName("test");
			change.setCustomMigrationScript("script");
			change.setListType("html");
			change.setType("list");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("html", model.getProperty(SchemaChangeModel.LIST_TYPE_KEY));
			assertEquals("list", model.getProperty(SchemaChangeModel.TYPE_KEY));
			assertEquals("script", model.getMigrationScript());
		}
	}

}
