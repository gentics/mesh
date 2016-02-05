package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.data.schema.UpdateSchemaChange;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class SchemaMutatorTest extends AbstractEmptyDBTest {

	@Autowired
	private SchemaMutator mutator;

	@Test
	public void testNullOperation() {
		Schema schema = new SchemaImpl();
		Schema updatedSchema = mutator.apply(schema, null);
		fail("Define error handling");
	}

	@Test
	public void testAddField() {
		Schema schema = new SchemaImpl();
		AddFieldChange change = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		change.setFieldName("name");
		change.setType("html");
		Schema updatedSchema = mutator.apply(schema, change);
		assertThat(updatedSchema).hasField("name");
	}

	@Test
	public void testRemoveField() {

		// 1. Create schema with field
		Schema schema = new SchemaImpl();
		schema.addField(new StringFieldSchemaImpl().setName("test"));

		// 2. Create remove field change
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName("test");

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(schema, change);

		assertThat(updatedSchema).hasNoField("test");
	}

	@Test
	public void testRemoveNonExistingField() {
		Schema schema = new SchemaImpl();
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName("test");

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(schema, change);

		fail("TODO define result");
	}

	@Test
	public void testUpdateField() {

		// 1. Create schema
		Schema schema = new SchemaImpl();
		BinaryFieldSchema field = new BinaryFieldSchemaImpl();
		field.setName("test");
		field.setAllowedMimeTypes("oldTypes");
		schema.addField(field);

		// 2. Create schema field update change
		UpdateFieldChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		change.setFieldName("test");
		change.setFieldProperty("allowedMimeTypes", "newTypes");

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(schema, change);
		BinaryFieldSchema updatedField = updatedSchema.getField("test", BinaryFieldSchemaImpl.class);
		assertNotNull(updatedField);
		assertArrayEquals(new String[] { "newTypes" }, updatedField.getAllowedMimeTypes());

	}

	@Test
	public void testUpdateNonExistingField() {
		fail("implement me");
	}

	@Test
	public void testUpdateSchema() {

		// 1. Create schema
		Schema schema = new SchemaImpl();

		// 2. Create schema update change
		UpdateSchemaChange change = Database.getThreadLocalGraph().addFramedVertex(UpdateSchemaChangeImpl.class);
		change.setDisplayField("newDisplayField");
		change.setContainerFlag(true);
		change.setSegmentField("newSegmentField");

		// 3. Apply the change
		Schema updatedSchema = mutator.apply(schema, change);
		assertEquals("The display field name was not updated", "newDisplayField", updatedSchema.getDisplayField());
		assertEquals("The segment field name was not updated", "newSegmentField", updatedSchema.getSegmentField());
		assertTrue("The schema container flag was not updated", updatedSchema.isContainer());
	}

}
