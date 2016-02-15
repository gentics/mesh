package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.FieldUtil;

/**
 * Test {@link UpdateSchemaChangeImpl} methods
 */
public class UpdateSchemaChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		assertNull("Initially no container flag value should be set.", change.getContainerFlag());
		change.setContainerFlag(true);
		assertTrue("The container flag should be set to true.", change.getContainerFlag());

		change.setCustomMigrationScript("test");
		assertEquals("test", change.getMigrationScript());

		change.setDescription("testDescription");
		assertEquals("testDescription", change.getDescription());

		change.setName("someName");
		assertEquals("someName", change.getName());

		change.setDisplayField("fieldName1");
		assertEquals("fieldName1", change.getDisplayField());

		change.setSegmentField("fieldName2");
		assertEquals("fieldName2", change.getSegmentField());
	}

	@Test
	public void testApply() {
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

	@Test
	@Override
	public void testUpdateFromRest() throws IOException {
		SchemaChangeModel model = new SchemaChangeModel();
		model.setMigrationScript("custom");
		model.setRequired(true);
		model.setProperty(SchemaChangeModel.CONTAINER_FIELD_KEY, true);
		model.setProperty(SchemaChangeModel.DESCRIPTION_KEY, "description");
		model.setProperty(SchemaChangeModel.SEGMENT_FIELD_KEY, "segmentField");
		model.setProperty(SchemaChangeModel.DISPLAY_FIELD_NAME_KEY, "displayField");
		model.setProperty(SchemaChangeModel.NAME_KEY, "newName");
		model.setProperty(SchemaChangeModel.FIELD_ORDER_KEY, new String[] { "A", "B", "C" });
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.updateFromRest(model);
		assertEquals("custom", change.getMigrationScript());
		assertTrue("The required flag should be set to true.", change.getRestProperty(REQUIRED_KEY));
		assertEquals("description", change.getDescription());
		assertEquals("segmentField", change.getSegmentField());
		assertEquals("displayField", change.getDisplayField());
		assertEquals("newName", change.getName());
		assertTrue("Container flag should have been set.", change.getContainerFlag());
		assertEquals(UpdateSchemaChange.OPERATION, change.getOperation());
		assertArrayEquals(new String[] { "A", "B", "C" }, change.getOrder().toArray());
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		assertNull("Update field changes have no auto migation script.", change.getAutoMigrationScript());

		assertNull("Intitially no migration script should be set.", change.getMigrationScript());
		change.setCustomMigrationScript("test");
		assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());

	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		SchemaChangeModel model = change.transformToRest();
		assertNotNull(model);
		assertEquals(UpdateSchemaChange.OPERATION, model.getOperation());
		assertEquals(change.getUuid(), model.getUuid());

		// Add more custom values
		change.setDisplayField("test");
		change.setSegmentField("test2");
		change.setCustomMigrationScript("script");
		change.setName("someName");
		change.setDescription("someDescription");
		change.setContainerFlag(true);

		// Transform and assert
		model = change.transformToRest();
		assertEquals("test", model.getProperty(SchemaChangeModel.DISPLAY_FIELD_NAME_KEY));
		assertEquals("test2", model.getProperty(SchemaChangeModel.SEGMENT_FIELD_KEY));
		assertEquals("script", model.getMigrationScript());
		assertEquals("someName", model.getProperty(SchemaChangeModel.NAME_KEY));
		assertEquals("someDescription", model.getProperty(SchemaChangeModel.DESCRIPTION_KEY));
		assertTrue("The container flag should have been set.", model.getProperty(SchemaChangeModel.CONTAINER_FIELD_KEY));

	}
}
