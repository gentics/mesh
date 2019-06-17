package com.gentics.mesh.rest;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = FULL, startServer = false)
public class RestModelTest extends AbstractMeshTest {

	@Test
	public void testNodeResponse() throws JsonParseException, JsonMappingException, IOException {

		meshDagger().serverSchemaStorage().addSchema(getDummySchema());
		NodeResponse response = new NodeResponse();
		StringField stringField = new StringFieldImpl();
		stringField.setString("some text");
		
		FieldMap fields = new FieldMapImpl();
		fields.put("name", stringField);
		response.setFields(fields);
		response.setSchema(new SchemaReferenceImpl().setName("content").setUuid(UUIDUtil.randomUUID()));
		String json = response.toJson();
		assertNotNull(json);

		NodeResponse deserializedResponse = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(deserializedResponse);
	}

	private SchemaModelImpl getDummySchema() {
		SchemaModelImpl schema = new SchemaModelImpl();
		schema.setName("content");
		schema.setDisplayField("title");
		//		schema.setMeshVersion(Mesh.getVersion());

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		schema.addField(titleFieldSchema);

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		schema.addField(nameFieldSchema);

		schema.setContainer(false);
		return schema;
	}

	@Test
	public void testNodeCreateRequest() throws JsonParseException, JsonMappingException, IOException {

		// schema
		ClientSchemaStorage schemaStorage = new ClientSchemaStorage();
		schemaStorage.addSchema(getDummySchema());

		NodeCreateRequest request = new NodeCreateRequest();

		request.setSchema(new SchemaReferenceImpl().setName("content").setUuid(UUIDUtil.randomUUID()));
		request.setLanguage("en");
		request.setParentNodeUuid(UUIDUtil.randomUUID());

		StringField stringField = new StringFieldImpl();
		stringField.setString("some text");
		request.getFields().put("name", stringField);

		StringField titleField = new StringFieldImpl();
		titleField.setString("The awesome title");
		request.getFields().put("title", titleField);

		// Serialize the NodeCreateRequest
		String json = request.toJson();
		System.out.println(json);

		// Deserialize the json and extract the schema info
		SchemaReferenceInfo info = JsonUtil.readValue(json, SchemaReferenceInfo.class);
		assertNotNull(info.getSchema());
		assertNotNull(info.getSchema().getName());
		assertNotNull(info.getSchema().getUuid());
		assertEquals("content", info.getSchema().getName());

		// Deserialize NodeCreateRequest using the schema info
		NodeCreateRequest loadedRequest = JsonUtil.readValue(json, NodeCreateRequest.class);
		FieldMap fields = loadedRequest.getFields();
		assertNotNull(fields);
		assertNotNull(fields.hasField("name"));
		assertNotNull(fields.getStringField("name").getString());
		assertEquals(StringFieldImpl.class.getName(), fields.getStringField("name").getClass().getName());

	}

	@Test
	public void testNodeList() throws Exception {
		try (Tx tx = tx()) {
			SchemaModel folderSchema = schemaContainer("folder").getLatestVersion().getSchema();
			SchemaModel contentSchema = schemaContainer("content").getLatestVersion().getSchema();

			NodeResponse folder = new NodeResponse();
			FieldMap folderFields = new FieldMapImpl();
			folder.setSchema(new SchemaReferenceImpl().setName(folderSchema.getName()));
			folderFields.put("name", FieldUtil.createStringField("folder name"));
			folder.setFields(folderFields);
			// folder.getFields().put("displayName", FieldUtil.createStringField("folder display name"));

			NodeResponse content = new NodeResponse();
			content.setSchema(new SchemaReferenceImpl().setName(contentSchema.getName()));
			FieldMap contentFields = new FieldMapImpl();
			
			contentFields.put("name", FieldUtil.createStringField("content name"));
			contentFields.put("content", FieldUtil.createStringField("some content"));
			content.setFields(contentFields);

			SchemaStorage storage = new ClientSchemaStorage();
			storage.addSchema(folderSchema);
			storage.addSchema(contentSchema);

			NodeListResponse list = new NodeListResponse();
			list.getData().add(folder);
			list.getData().add(content);
			String json = list.toJson();
			NodeListResponse deserializedList = JsonUtil.readValue(json, NodeListResponse.class);
			assertNotNull(deserializedList);
		}
	}

	@Test
	public void testSchema() throws JsonParseException, JsonMappingException, IOException {

		Schema schemaCreateRequest = new SchemaModelImpl();
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
		//		listFieldSchema.setMin(5);
		//		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });
		// NodeField defaultNode = new NodeFieldImpl();
		// defaultNode.setUuid(UUIDUtil.randomUUID());
		// listFieldSchema.getItems().add(defaultNode);
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
		String json = schemaCreateRequest.toJson();
		assertNotNull(json);

		// Deserialize the object
		Schema loadedRequest = JsonUtil.readValue(json, SchemaModelImpl.class);
		assertNotNull(loadedRequest);

		// Serialize the object
		String json2 = loadedRequest.toJson();
		assertEquals(json, json2);

	}

	@Test
	public void testNodeSchema2() {
		Schema schema = new SchemaModelImpl();

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
		//		listFieldSchema.setMin(5);
		//		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });

		for (FieldSchema fieldEntry : schema.getFields()) {
			System.out.println("Schema Field: " + fieldEntry.getName() + " has " + fieldEntry.getType());
			System.out.println();
		}

	}
}
