package com.gentics.mesh.core.schema.change;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.FieldUtil;

/**
 * Test {@link UpdateFieldChangeImpl} methods.
 */
public class UpdateFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.setLabel("testLabel");
		assertEquals("testLabel", change.getLabel());
	}

	@Test
	@Override
	public void testApply() {
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
	@Override
	public void testUpdateFromRest() {
		SchemaChangeModel model = new SchemaChangeModel();
		model.setOperation(UpdateFieldChange.OPERATION);
		model.setProperty(SchemaChangeModel.FIELD_NAME_KEY, "someField");
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.updateFromRest(model);
		assertEquals("someField", change.getFieldName());
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		assertNull("Update field changes have no auto migation script.", change.getAutoMigrationScript());

		assertNull("Intitially no migration script should be set.", change.getMigrationScript());
		change.setCustomMigrationScript("test");
		assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.setFieldName("fieldName");

		SchemaChangeModel model = change.transformToRest();
		assertEquals("fieldName", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
		assertEquals(UpdateFieldChange.OPERATION, model.getOperation());
	}

}
