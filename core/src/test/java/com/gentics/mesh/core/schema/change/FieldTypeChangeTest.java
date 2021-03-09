package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class FieldTypeChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (Tx tx = tx()) {
			FieldTypeChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			change.setFieldName("name");
			assertEquals("name", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (Tx tx = tx()) {
			SchemaVersion version = ((GraphDBTx) tx).getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.setName("testschema");

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setName("stringField");
			stringField.setRequired(true);
			schema.addField(stringField);

			FieldTypeChange fieldTypeUpdate = ((GraphDBTx) tx).getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("html");

			// 3. Apply the changes
			version.setNextChange(fieldTypeUpdate);
			version.setSchema(schema);

			SchemaModel updatedSchema = mutator.apply(version);
			assertNotNull(updatedSchema);
			assertEquals("html", updatedSchema.getField("stringField").getType());
		}
	}

	@Test
	public void testChangeFieldTypeToList() {
		try (Tx tx = tx()) {
			SchemaVersion version = ((GraphDBTx) tx).getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			// 1. Create schema
			SchemaModelImpl schema = new SchemaModelImpl();
			schema.setName("testschema");

			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setName("stringField");
			stringField.setRequired(true);
			stringField.setLabel("test123");
			schema.addField(stringField);

			FieldTypeChange fieldTypeUpdate = ((GraphDBTx) tx).getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			fieldTypeUpdate.setFieldName("stringField");
			fieldTypeUpdate.setType("list");
			fieldTypeUpdate.setListType("html");

			version.setNextChange(fieldTypeUpdate);
			version.setSchema(schema);

			// 3. Apply the changes
			SchemaModel updatedSchema = mutator.apply(version);
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
			model.setProperty(SchemaChangeModel.LIST_TYPE_KEY, "html");
			FieldTypeChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			change.updateFromRest(model);

			assertEquals("testField", change.getFieldName());
			assertEquals("list", change.getType());
			assertEquals("html", change.getListType());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (Tx tx = tx()) {
			FieldTypeChange change = ((GraphDBTx) tx).getGraph().addFramedVertex(FieldTypeChangeImpl.class);
			change.setFieldName("test");
			change.setListType("html");
			change.setType("list");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("html", model.getProperty(SchemaChangeModel.LIST_TYPE_KEY));
			assertEquals("list", model.getProperty(SchemaChangeModel.TYPE_KEY));
		}
	}

}
