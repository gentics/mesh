package com.gentics.mesh.core.field;

import java.io.IOException;

import org.junit.Before;

import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;

public abstract class AbstractListFieldEndpointTest extends AbstractFieldEndpointTest implements FieldEndpointTestcases, ListFieldEndpointTestcases {

	protected static final String FIELD_NAME = "listField";

	public abstract String getListFieldType();

	@Before
	public void updateSchema() throws IOException {
		try (NoTx noTx = db.noTx()) {
			setSchema(getListFieldType());
		}
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
