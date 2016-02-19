package com.gentics.mesh.core.field.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class HtmlGraphFieldTest extends AbstractEmptyDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testSimpleHTML() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField field = new HtmlGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-html"));
		field.setHtml("dummy HTML");
		assertEquals("dummy HTML", field.getHTML());
		assertEquals("dummy HTML", container.getProperty("test-html"));
		assertEquals(3, container.getPropertyKeys().size());
		field.setHtml(null);
		assertNull(field.getHTML());
		assertNull(container.getProperty("test-html"));

		HtmlGraphField reloadedField = container.getHtml("test");
		assertNull("The html field value was set to null and thus was removed", reloadedField);
	}

	@Test
	public void testHTMLField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField htmlField = container.createHTML("htmlField");
		assertEquals("htmlField", htmlField.getFieldKey());
		htmlField.setHtml("dummyHTML");
		assertEquals("dummyHTML", htmlField.getHTML());
		HtmlGraphField bogusField1 = container.getHtml("bogus");
		assertNull(bogusField1);
		HtmlGraphField reloadedHTMLField = container.getHtml("htmlField");
		assertNotNull(reloadedHTMLField);
		assertEquals("htmlField", reloadedHTMLField.getFieldKey());
	}

	@Test
	public void testHtmlFieldTransformation() throws Exception {
		setupData();
		Node node = folder("2015");
		Schema schema = node.getSchemaContainer().getSchema();
		HtmlFieldSchemaImpl htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("htmlField");
		htmlFieldSchema.setLabel("Some html field");
		htmlFieldSchema.setRequired(true);
		schema.addField(htmlFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		HtmlGraphField field = container.createHTML("htmlField");
		field.setHtml("Some<b>htmlABCDE");

		String json = getJson(node);
		assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("ABCDE") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.HtmlField deserializedNodeField = response.getField("htmlField", HtmlFieldImpl.class);
		assertNotNull(deserializedNodeField);
		assertEquals("Some<b>htmlABCDE", deserializedNodeField.getHTML());

	}
}
