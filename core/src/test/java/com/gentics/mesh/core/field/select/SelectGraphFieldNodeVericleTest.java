package com.gentics.mesh.core.field.select;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;

public class SelectGraphFieldNodeVericleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		SelectFieldSchema selectFieldSchema = new SelectFieldSchemaImpl();
		selectFieldSchema.setName("selectField");
		selectFieldSchema.setLabel("Some label");
		schema.addField(selectFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		throw new NotImplementedException();

	}

}
