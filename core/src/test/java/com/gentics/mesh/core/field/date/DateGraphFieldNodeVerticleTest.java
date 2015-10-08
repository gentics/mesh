package com.gentics.mesh.core.field.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;

public class DateGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
		dateFieldSchema.setName("dateField");
		dateFieldSchema.setLabel("Some label");
		schema.addField(dateFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode("dateField", (Field) null);
		DateFieldImpl field = response.getField("dateField");
		assertNotNull(field);
		assertNull(field.getDate());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeResponse response = updateNode("dateField", new DateFieldImpl().setDate(nowEpoch));
		DateFieldImpl field = response.getField("dateField");
		assertEquals(nowEpoch, field.getDate());

		response = updateNode("dateField", new DateFieldImpl().setDate(nowEpoch));
		field = response.getField("dateField");
		assertEquals(nowEpoch, field.getDate());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeResponse response = createNode("dateField", new DateFieldImpl().setDate(nowEpoch));
		DateField field = response.getField("dateField");
		assertEquals(nowEpoch, field.getDate());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;

		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createDate("dateField").setDate(nowEpoch);

		NodeResponse response = readNode(node);
		DateField deserializedDateField = response.getField("dateField");
		assertNotNull(deserializedDateField);
		assertEquals(nowEpoch, deserializedDateField.getDate());
	}
}
