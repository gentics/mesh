package com.gentics.mesh.core.field.select;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;

public class SelectGraphFieldNodeVericleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		try (Trx tx = new Trx(db)) {
			Schema schema = schemaContainer("folder").getSchema();
			SelectFieldSchema selectFieldSchema = new SelectFieldSchemaImpl();
			selectFieldSchema.setName("selectField");
			selectFieldSchema.setLabel("Some label");
			schema.addField(selectFieldSchema);
			schemaContainer("folder").setSchema(schema);
		}
	}

	@Test
	@Override
	@Ignore
	public void testUpdateNodeFieldWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	@Ignore
	public void testCreateNodeWithField() {
		throw new NotImplementedException();
	}

	@Test
	@Override
	@Ignore
	public void testReadNodeWithExitingField() {
		throw new NotImplementedException();
	}

}
