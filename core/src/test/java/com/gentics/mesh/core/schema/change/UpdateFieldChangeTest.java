package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Test {@link UpdateFieldChangeImpl} methods.
 */
@MeshTestSetting(useElasticsearch = false, useTinyDataset = true, startServer = false)
public class UpdateFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setLabel("testLabel");
			assertEquals("testLabel", change.getLabel());
		}
	}

	@Test
	@Override
	public void testApply() {
		try (NoTx noTx = db().noTx()) {
			SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			SchemaModel schema = new SchemaModel("test");
			schema.addField(FieldUtil.createStringFieldSchema("name"));

			UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setFieldName("name");
			change.setLabel("updated");
			version.setSchema(schema);
			version.setNextChange(change);

			FieldSchemaContainer updatedSchema = mutator.apply(version);
			assertEquals("The field label was not updated by the mutator.", "updated", updatedSchema.getField("name").getLabel());
		}
	}

	@Test
	@Override
	public void testUpdateFromRest() {
		try (NoTx noTx = db().noTx()) {
			SchemaChangeModel model = new SchemaChangeModel(UPDATEFIELD, "someField");
			UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.updateFromRest(model);
			assertEquals("someField", change.getFieldName());
		}
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			assertNull("Update field changes have no auto migation script.", change.getAutoMigrationScript());

			assertNull("Intitially no migration script should be set.", change.getMigrationScript());
			change.setCustomMigrationScript("test");
			assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
		}
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		try (NoTx noTx = db().noTx()) {
			UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			change.setFieldName("fieldName");

			SchemaChangeModel model = change.transformToRest();
			assertEquals("fieldName", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
			assertEquals(UpdateFieldChange.OPERATION, model.getOperation());
		}
	}

}
