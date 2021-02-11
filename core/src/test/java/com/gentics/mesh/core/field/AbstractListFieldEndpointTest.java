package com.gentics.mesh.core.field;

import java.io.IOException;

import org.junit.Before;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public abstract class AbstractListFieldEndpointTest extends AbstractFieldEndpointTest implements FieldEndpointTestcases, ListFieldEndpointTestcases {

	protected static final String FIELD_NAME = "listField";

	public abstract String getListFieldType();

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			setSchema(getListFieldType());
		}
	}

	protected void setSchema(String listType) throws IOException {
		SchemaVersionModel schema = schemaContainer("folder").getLatestVersion().getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELD_NAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		schema.removeField(FIELD_NAME);
		schema.addField(listFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

}
