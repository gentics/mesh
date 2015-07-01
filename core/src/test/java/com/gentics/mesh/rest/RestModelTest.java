package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.UUIDUtil;

public class RestModelTest extends AbstractDBTest {

	@Test
	public void testNodeResponse() {
		NodeResponse response = new NodeResponse();
		StringField stringField = new StringFieldImpl();
		stringField.setText("some text");
		response.getFields().put("name", stringField);

		System.out.println(JsonUtil.toJson(response));

	}

	@Test
	public void testNodeCreateRequest() throws JsonParseException, JsonMappingException, IOException {

		// schema
		Schema schema = new SchemaImpl();
		schema.setName("content");
		schema.setDisplayField("title");
		schema.setMeshVersion(Mesh.getVersion());
		schema.setSchemaVersion("1.0.0");

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		titleFieldSchema.setText("Enter the title here");
		schema.addField("title", titleFieldSchema);

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		nameFieldSchema.setText("Enter the name here");
		schema.addField("name", nameFieldSchema);

		schema.setBinary(false);
		schema.setContainer(false);

		// request
		NodeCreateRequest request = new NodeCreateRequest();

		request.setSchema(new SchemaReference("content", UUIDUtil.randomUUID()));
		request.setLanguage("en");
		request.setParentNodeUuid(UUIDUtil.randomUUID());

		StringField stringField = new StringFieldImpl();
		stringField.setText("some text");
		request.getFields().put("name", stringField);

		StringField titleField = new StringFieldImpl();
		titleField.setText("The awesome title");
		request.getFields().put("title", titleField);

		String json = JsonUtil.toJson(request);
		System.out.println(json);

		SchemaReferenceInfo info = JsonUtil.readValue(json, SchemaReferenceInfo.class);
		assertNotNull(info.getSchema());
		assertNotNull(info.getSchema().getName());
		assertNotNull(info.getSchema().getUuid());
		assertEquals("content", info.getSchema().getName());

		NodeCreateRequest loadedRequest = JsonUtil.readValue(json, NodeCreateRequest.class, schema);
		Map<String, Field> fields = loadedRequest.getFields();
		assertNotNull(fields);
		assertNotNull(fields.get("name"));
		assertEquals(StringFieldImpl.class.getName(), fields.get("name").getClass().getName());

	}

}
