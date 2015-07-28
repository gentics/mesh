package com.gentics.mesh.core.field.html;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;

public class HtmlFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		HtmlFieldSchema htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName("htmlField");
		htmlFieldSchema.setLabel("Some label");
		schema.addField(htmlFieldSchema);
		schemaContainer("folder").setSchema(schema);
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
		throw new NotImplementedException();
	}

}
