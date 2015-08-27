package com.gentics.mesh.core.field.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;

public class DateGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		try (Trx tx = db.trx()) {
			Schema schema = schemaContainer("folder").getSchema();
			DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
			dateFieldSchema.setName("dateField");
			dateFieldSchema.setLabel("Some label");
			schema.addField(dateFieldSchema);
			schemaContainer("folder").setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		try (Trx tx = db.trx()) {
			NodeResponse response = updateNode("dateField", new DateFieldImpl().setDate("01.01.1971"));
			DateFieldImpl field = response.getField("dateField");
			assertEquals("01.01.1971", field.getDate());

			response = updateNode("dateField", new DateFieldImpl().setDate("02.01.1971"));
			field = response.getField("dateField");
			assertEquals("02.01.1971", field.getDate());
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Trx tx = db.trx()) {
			NodeResponse response = createNode("dateField", new DateFieldImpl().setDate("01.01.1971"));
			DateField field = response.getField("dateField");
			assertEquals("01.01.1971", field.getDate());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node;
		try (Trx tx = db.trx()) {
			node = folder("2015");
			NodeGraphFieldContainer container = node.getFieldContainer(english());
			container.createDate("dateField").setDate("01.01.1971");
			tx.success();
		}

		try (Trx tx = db.trx()) {
			NodeResponse response = readNode(node);
			DateField deserializedDateField = response.getField("dateField");
			assertNotNull(deserializedDateField);
			assertEquals("01.01.1971", deserializedDateField.getDate());
		}
	}
}
