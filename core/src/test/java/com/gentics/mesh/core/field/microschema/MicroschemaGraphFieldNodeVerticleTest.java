package com.gentics.mesh.core.field.microschema;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;

public class MicroschemaGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		microschemaFieldSchema.setName("microschemaField");
		microschemaFieldSchema.setLabel("Some label");
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode("microschemaField", (Field) null);
		MicroschemaField field = response.getField("microschemaField");
		assertNotNull(field);
		assertNull(field.getFields());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		throw new NotImplementedException();
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateNodeWithField() {
		throw new NotImplementedException();
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testReadNodeWithExitingField() throws IOException {
		throw new NotImplementedException();
	}

}
