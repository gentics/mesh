package com.gentics.mesh.core.field.list;

import java.io.IOException;

import org.junit.Before;

import com.gentics.mesh.core.field.AbstractBasicFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public abstract class AbstractGraphListFieldVerticleTest extends AbstractBasicFieldNodeVerticleTest {

	protected static final String FIELD_NAME = "listField";

	abstract String getListFieldType();

	@Before
	public void updateSchema() throws IOException {
		setSchema(getListFieldType());
	}

	protected void setSchema(String listType) throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELD_NAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		schema.removeField(FIELD_NAME);
		schema.addField(listFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	/**
	 * Update a node with a currently filled field using a null value. Assert that the field will be deleted and is no longer listed in the response.
	 */
	abstract public void testUpdateSetNull();

	/**
	 * Update a node with a currently filled field using a empty field value. Assert that the field will still be listed in the response. It should not be
	 * deleted. Some basic field types can't be set to empty (e.g: Number, Date, Boolean..) Assert that the field will be set to null in those cases.
	 */
	abstract public void testUpdateSetEmpty();
	
}
