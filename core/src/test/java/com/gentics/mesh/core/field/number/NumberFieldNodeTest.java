package com.gentics.mesh.core.field.number;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class NumberFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

	@Test
	public void testNumberFieldTransformation() throws IOException, InterruptedException {
		Node node = folder("2015");
		Schema schema = node.getSchema();
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("numberField");
		numberFieldSchema.setMin(10);
		numberFieldSchema.setMax(1000);
		numberFieldSchema.setRequired(true);
		schema.addField(numberFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeFieldContainer container = node.getFieldContainer(english());

		NumberGraphField numberField = container.createNumber("numberField");
		numberField.setNumber("100.9");

		String json = getJson(node);
		assertTrue(json.indexOf("100.9") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);
		NumberFieldImpl deserializedNumberField = response.getField("numberField");
		assertEquals("100.9", deserializedNumberField.getNumber());
	}
}
