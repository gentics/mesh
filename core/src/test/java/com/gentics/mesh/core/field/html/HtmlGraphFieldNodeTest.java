package com.gentics.mesh.core.field.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class HtmlGraphFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testHtmlFieldTransformation() throws IOException, InterruptedException {
		Node node = folder("2015");
		Schema schema = node.getSchema();
		HtmlFieldSchemaImpl htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("htmlField");
		htmlFieldSchema.setLabel("Some html field");
		htmlFieldSchema.setRequired(true);
		schema.addField(htmlFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeFieldContainer container = node.getFieldContainer(english());
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