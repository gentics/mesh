package com.gentics.mesh.core.field.microschema;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;

public class MicroschemaFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		microschemaFieldSchema.setName("microschemaField");
		microschemaFieldSchema.setLabel("Some label");
		schema.addField(microschemaFieldSchema);
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
	public void testReadNodeWithExitingField() throws IOException {
		throw new NotImplementedException();
	}

}
