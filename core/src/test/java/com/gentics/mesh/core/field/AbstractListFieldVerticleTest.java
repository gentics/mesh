package com.gentics.mesh.core.field;

import java.io.IOException;

import org.junit.Before;

import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public abstract class AbstractListFieldVerticleTest extends AbstractFieldVerticleTest implements FieldVerticleTestcases {

	protected static final String FIELD_NAME = "listField";

	public abstract String getListFieldType();

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

}
