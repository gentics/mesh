package com.gentics.mesh.core.field.html;

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
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;

public class HtmlGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("htmlField");
		htmlFieldSchema.setLabel("Some label");
		schema.addField(htmlFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNodeAndCheck(null, (Field) null);
		HtmlFieldImpl htmlField = response.getFields().getHtmlField("htmlField");
		assertNotNull(htmlField);
		assertNull(htmlField.getHTML());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("htmlField", new HtmlFieldImpl().setHTML("some<b>html"));
		HtmlFieldImpl field = response.getFields().getHtmlField("htmlField");
		assertEquals("some<b>html", field.getHTML());

		response = updateNode("htmlField", new HtmlFieldImpl().setHTML("some<b>html2"));
		field = response.getFields().getHtmlField("htmlField");
		assertEquals("some<b>html2", field.getHTML());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNodeAndCheck("htmlField", new HtmlFieldImpl().setHTML("Some<b>html"));
		HtmlFieldImpl htmlField = response.getFields().getHtmlField("htmlField");
		assertEquals("Some<b>html", htmlField.getHTML());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createHTML("htmlField").setHtml("some<b>html");

		NodeResponse response = readNode(node);
		HtmlFieldImpl deserializedHtmlField = response.getFields().getHtmlField("htmlField");
		assertNotNull(deserializedHtmlField);
		assertEquals("some<b>html", deserializedHtmlField.getHTML());

	}

}
