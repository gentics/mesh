package com.gentics.mesh.core.field;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;

import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public abstract class AbstractListFieldEndpointTest extends AbstractFieldEndpointTest implements FieldEndpointTestcases, ListFieldEndpointTestcases {

	protected static final String FIELD_NAME = "listField";

	public abstract String getListFieldType();

	@Before
	public void updateSchema() throws IOException {
		tx(() -> setSchema(getListFieldType()));
	}

	protected void setSchema(String listType) throws IOException {
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELD_NAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		prepareTypedSchema(schemaContainer("folder"), List.of(listFieldSchema), Optional.empty());
	}

}
