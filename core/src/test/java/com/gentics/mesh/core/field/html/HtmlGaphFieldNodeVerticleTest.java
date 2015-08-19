package com.gentics.mesh.core.field.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;

public class HtmlGaphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		try (Trx tx = new Trx(db)) {
			Schema schema = schemaContainer("folder").getSchema();
			HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName("htmlField");
			htmlFieldSchema.setLabel("Some label");
			schema.addField(htmlFieldSchema);
			schemaContainer("folder").setSchema(schema);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("htmlField", new HtmlFieldImpl().setHTML("some<b>html"));
		HtmlFieldImpl field = response.getField("htmlField");
		assertEquals("some<b>html", field.getHTML());

		response = updateNode("htmlField", new HtmlFieldImpl().setHTML("some<b>html2"));
		field = response.getField("htmlField");
		assertEquals("some<b>html2", field.getHTML());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("htmlField", new HtmlFieldImpl().setHTML("Some<b>html"));
		HtmlFieldImpl htmlField = response.getField("htmlField");
		assertEquals("Some<b>html", htmlField.getHTML());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		container.createHTML("htmlField").setHtml("some<b>html");

		NodeResponse response = readNode(node);
		HtmlFieldImpl deserializedHtmlField = response.getField("htmlField");
		assertNotNull(deserializedHtmlField);
		assertEquals("some<b>html", deserializedHtmlField.getHTML());

	}

}
