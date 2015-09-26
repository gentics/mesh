package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.FieldUtil;
import com.gentics.mesh.util.UUIDUtil;

public class RestModelTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testNodeResponse() throws JsonParseException, JsonMappingException, IOException {

		schemaStorage.addSchema(getDummySchema());
		NodeResponse response = new NodeResponse();
		StringField stringField = new StringFieldImpl();
		stringField.setString("some text");
		response.getFields().put("name", stringField);
		response.setSchema(new SchemaReference("content", UUIDUtil.randomUUID()));
		String json = JsonUtil.toJson(response);
		assertNotNull(json);

		NodeResponse deserializedResponse = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(deserializedResponse);
	}

	private Schema getDummySchema() {
		Schema schema = new SchemaImpl();
		schema.setName("content");
		schema.setDisplayField("title");
		schema.setMeshVersion(Mesh.getVersion());

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		schema.addField(titleFieldSchema);

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		schema.addField(nameFieldSchema);

		schema.setBinary(false);
		schema.setFolder(false);
		return schema;
	}

	@Test
	public void testNodeCreateRequest() throws JsonParseException, JsonMappingException, IOException {

		// schema
		ClientSchemaStorage schemaStorage = new ClientSchemaStorage();
		schemaStorage.addSchema(getDummySchema());

		NodeCreateRequest request = new NodeCreateRequest();

		request.setSchema(new SchemaReference("content", UUIDUtil.randomUUID()));
		request.setLanguage("en");
		request.setParentNodeUuid(UUIDUtil.randomUUID());

		StringField stringField = new StringFieldImpl();
		stringField.setString("some text");
		request.getFields().put("name", stringField);

		StringField titleField = new StringFieldImpl();
		titleField.setString("The awesome title");
		request.getFields().put("title", titleField);

		// Serialize the NodeCreateRequest
		String json = JsonUtil.toJson(request);
		System.out.println(json);

		// Deserialize the json and extract the schema info
		SchemaReferenceInfo info = JsonUtil.readValue(json, SchemaReferenceInfo.class);
		assertNotNull(info.getSchema());
		assertNotNull(info.getSchema().getName());
		assertNotNull(info.getSchema().getUuid());
		assertEquals("content", info.getSchema().getName());

		// Deserialize NodeCreateRequest using the schema info
		NodeCreateRequest loadedRequest = JsonUtil.readNode(json, NodeCreateRequest.class, schemaStorage);
		Map<String, Field> fields = loadedRequest.getFields();
		assertNotNull(fields);
		assertNotNull(fields.get("name"));
		assertNotNull(((StringField) fields.get("name")).getString());
		assertEquals(StringFieldImpl.class.getName(), fields.get("name").getClass().getName());

	}

	@Test
	public void testNodeList() throws Exception {
		setupData();
		try (Trx tx = db.trx()) {
			Schema folderSchema = schemaContainer("folder").getSchema();
			Schema contentSchema = schemaContainer("content").getSchema();

			NodeResponse folder = new NodeResponse();
			folder.setSchema(new SchemaReference(folderSchema.getName(), null));
			folder.getFields().put("name", FieldUtil.createStringField("folder name"));
			//		folder.getFields().put("displayName", FieldUtil.createStringField("folder display name"));

			NodeResponse content = new NodeResponse();
			content.setSchema(new SchemaReference(contentSchema.getName(), null));
			content.getFields().put("name", FieldUtil.createStringField("content name"));
			content.getFields().put("content", FieldUtil.createStringField("some content"));

			SchemaStorage storage = new ClientSchemaStorage();
			storage.addSchema(folderSchema);
			storage.addSchema(contentSchema);

			NodeListResponse list = new NodeListResponse();
			list.getData().add(folder);
			list.getData().add(content);
			String json = JsonUtil.toJson(list);
			NodeListResponse deserializedList = JsonUtil.readNode(json, NodeListResponse.class, storage);
			assertNotNull(deserializedList);
		}
	}

	@Test
	public void testSchema() throws JsonParseException, JsonMappingException, IOException {

		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
		schemaCreateRequest.setName("blogpost");
		schemaCreateRequest.setDisplayField("name");

		assertEquals("name", schemaCreateRequest.getDisplayField());
		assertEquals("blogpost", schemaCreateRequest.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("name");
		schemaCreateRequest.addField(stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean");
		schemaCreateRequest.addField(booleanSchema);

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("list");
		listFieldSchema.setLabel("list field label");
		listFieldSchema.setListType("node");
		listFieldSchema.setMin(5);
		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });
		//NodeField defaultNode = new NodeFieldImpl();
		//defaultNode.setUuid(UUIDUtil.randomUUID());
		//listFieldSchema.getItems().add(defaultNode);
		schemaCreateRequest.addField(listFieldSchema);

		// MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		// microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "gallery", "shorttext" });
		// microschemaFieldSchema.setLabel("gallery label");
		// microschemaFieldSchema.setName("gallery name");
		//
		// NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		// nodeFieldSchema.setUuid(UUIDUtil.randomUUID());
		// nodeFieldSchema.setLabel("gallery node label");
		// nodeFieldSchema.setName("gallery node name");
		//
		// microschemaFieldSchema.getFields().add(nodeFieldSchema);
		// request.addField(microschemaFieldSchema);

		// schema = schemaStorage.getSchema(schema.getName());
		// assertNotNull(schema);

		// Serialize the object
		String json = JsonUtil.toJson(schemaCreateRequest);
		assertNotNull(json);

		// Deserialize the object
		SchemaCreateRequest loadedRequest = JsonUtil.readSchema(json, SchemaCreateRequest.class);
		assertNotNull(loadedRequest);

		// Serialize the object
		String json2 = JsonUtil.toJson(loadedRequest);
		assertEquals(json, json2);

	}

	@Test
	public void testNodeSchemaCreateRequest2() {
		Schema schema = new SchemaImpl();

		schema.setName("blogpost");
		schema.setDisplayField("name");

		// assertEquals("bogusUUID", request.getUuid());
		// assertEquals("name", request.getDisplayField());
		// assertEquals("blogpost", request.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("string");
		schema.addField(stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean");
		schema.addField(booleanSchema);

		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("list field name");
		listFieldSchema.setLabel("list field label");
		listFieldSchema.setListType("node");
		listFieldSchema.setMin(5);
		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });

		for (FieldSchema fieldEntry : schema.getFields()) {
			System.out.println("Schema Field: " + fieldEntry.getName() + " has " + fieldEntry.getType());
			System.out.println();
		}

	}
}
