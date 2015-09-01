package com.gentics.mesh.core.field.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;

public class NumberGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		try (Trx tx = db.trx()) {
			Schema schema = schemaContainer("folder").getSchema();
			NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
			numberFieldSchema.setName("numberField");
			numberFieldSchema.setMin(10);
			numberFieldSchema.setMax(1000);
			numberFieldSchema.setRequired(true);
			schema.addField(numberFieldSchema);
			schemaContainer("folder").setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		try (Trx tx = db.trx()) {
			NodeResponse response = updateNode("numberField", new NumberFieldImpl().setNumber("42"));
			NumberFieldImpl field = response.getField("numberField");
			assertEquals("42", field.getNumber());
		}
		try (Trx tx = db.trx()) {
			NodeResponse response = updateNode("numberField", new NumberFieldImpl().setNumber("43"));
			NumberFieldImpl field = response.getField("numberField");
			assertEquals("43", field.getNumber());
		}
	}

	@Test
	@Ignore("Not yet implemented")
	public void testCreateNodeWithBooleanFieldInsteadOfNumber() {
		//We expect the json serializer to fail.
		fail("not yet implemented");
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("numberField", new NumberFieldImpl().setNumber("1.21"));
		NumberFieldImpl numberField = response.getField("numberField");
		assertEquals("1.21", numberField.getNumber());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() throws IOException {
		Node node;
		try (Trx tx = db.trx()) {
			node = folder("2015");

			NodeGraphFieldContainer container = node.getFieldContainer(english());
			NumberGraphField numberField = container.createNumber("numberField");
			numberField.setNumber("100.9");
			tx.success();
		}

		try (Trx tx = db.trx()) {
			NodeResponse response = readNode(node);

			NumberFieldImpl deserializedNumberField = response.getField("numberField", NumberFieldImpl.class);
			assertNotNull(deserializedNumberField);
			assertEquals("100.9", deserializedNumberField.getNumber());
		}
	}

}
