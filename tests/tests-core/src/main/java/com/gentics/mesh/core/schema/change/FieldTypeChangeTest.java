package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.HibFieldTypeChange;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class FieldTypeChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			SchemaModelImpl schemaModel = new SchemaModelImpl("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema, v -> {});

			HibFieldTypeChange change = (HibFieldTypeChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.CHANGEFIELDTYPE);
			change.setFieldName("name");
			assertEquals("name", change.getFieldName());
		} catch (MeshSchemaException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			// 1. Create schema
			SchemaModelImpl schemaModel = new SchemaModelImpl();
			schemaModel.setName("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema, v -> {
				StringFieldSchema stringField = new StringFieldSchemaImpl();
				stringField.setName("stringField");
				stringField.setRequired(true);
				schemaModel.addField(stringField);
				v.setSchema(schemaModel);
			});

			HibFieldTypeChange fieldTypeUpdate = (HibFieldTypeChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.CHANGEFIELDTYPE);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("html");

			// 3. Apply the changes
			version.setNextChange(fieldTypeUpdate);

			SchemaModel updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			assertEquals("html", updatedSchema.getField("stringField").getType());
		} catch (MeshSchemaException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testChangeFieldTypeToList() {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			// 1. Create schema
			SchemaModelImpl schemaModel = new SchemaModelImpl();
			schemaModel.setName("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema, v -> {
				StringFieldSchema stringField = new StringFieldSchemaImpl();
				stringField.setName("stringField");
				stringField.setRequired(true);
				stringField.setLabel("test123");
				schemaModel.addField(stringField);
				v.setSchema(schemaModel);
			});

			HibFieldTypeChange fieldTypeUpdate = (HibFieldTypeChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.CHANGEFIELDTYPE);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("list");
			fieldTypeUpdate.setListType("html");

			version.setNextChange(fieldTypeUpdate);

			// 3. Apply the changes
			SchemaModel updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			ListFieldSchema fieldSchema = updatedSchema.getField("stringField", ListFieldSchemaImpl.class);
			assertEquals("list", fieldSchema.getType());
			assertEquals("html", fieldSchema.getListType());
			assertEquals("test123", fieldSchema.getLabel());
		} catch (MeshSchemaException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() throws IOException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			SchemaModelImpl schemaModel = new SchemaModelImpl();
			schemaModel.setName("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema, v -> {});

			SchemaChangeModel model = SchemaChangeModel.createChangeFieldTypeChange("testField", "list");
			model.setProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
			HibFieldTypeChange change = (HibFieldTypeChange) ctx.schemaDao().createChange(version, model);
			change.updateFromRest(model);

			assertEquals("testField", change.getFieldName());
			assertEquals("list", change.getType());
			assertEquals("html", change.getListType());
		} catch (MeshSchemaException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();

			SchemaModelImpl schemaModel = new SchemaModelImpl();
			schemaModel.setName("testschema");
			HibSchema schema = ctx.schemaDao().create(schemaModel, user());
			HibSchemaVersion version = ctx.schemaDao().createPersistedVersion(schema, v -> {});

			HibFieldTypeChange change = (HibFieldTypeChange) ctx.schemaDao().createPersistedChange(version, SchemaChangeOperation.CHANGEFIELDTYPE);
			change.setFieldName("test");
			change.setListType("html");
			change.setType("list");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("html", model.getProperty(SchemaChangeModel.LIST_TYPE_KEY));
			assertEquals("list", model.getProperty(SchemaChangeModel.TYPE_KEY));
		} catch (MeshSchemaException e) {
			throw new RuntimeException(e);
		}
	}

}
