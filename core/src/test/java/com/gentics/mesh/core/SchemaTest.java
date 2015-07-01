package com.gentics.mesh.core;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.SchemaStorage;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.UUIDUtil;

public class SchemaTest extends AbstractDBTest {

	@Autowired
	private SchemaContainerService schemaContainerService;

	@Autowired
	private SchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testFindByName() {
		SchemaContainer schema = schemaContainerService.findByName(PROJECT_NAME, "content");
		assertNotNull(schema);
		//		assertEquals("content", schema.getName());
		assertNull(schemaContainerService.findByName(PROJECT_NAME, "content1235"));
	}

	@Test
	public void testDeleteByObject() {
		SchemaContainer schema = data().getSchemaContainer("content");
		String uuid = schema.getUuid();
		schema.delete();
		assertNull(schemaContainerService.findByUUID(uuid));
	}

	// @Test
	// public void testDeleteWithNoPermission() {
	// UserInfo info = data().getUserInfo();
	// ObjectSchema schema = data().getContentSchema();
	// try (Transaction tx = graphDb.beginTx()) {
	// roleService.revokePermission(info.getRole(), schema, PermissionType.DELETE);
	// objectSchemaService.deleteByUUID(schema.getUuid());
	// tx.success();
	// }
	// assertNotNull(objectSchemaService.findOne(schema.getId()));
	// }

	@Test
	public void testObjectSchemaRootNode() {
		SchemaContainerRoot root = data().getMeshRoot().getSchemaContainerRoot();
		int nSchemasBefore = root.getSchemaContainers().size();
		assertNotNull(root.create("test1235"));
		int nSchemasAfter = root.getSchemaContainers().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}

	@Test
	public void testDefaultSchema() {
		SchemaContainerRoot root = data().getMeshRoot().getSchemaContainerRoot();
		assertEquals(4, root.getSchemaContainers().size());
	}

	@Test
	public void testSchemaStorage() {
		schemaStorage.clear();
		schemaStorage.init();
		Schema schema = schemaStorage.getSchema("folder");
		assertNotNull(schema);
		assertEquals("folder", schema.getName());
	}

	@Test
	public void testNodeSchemaCreateRequest() throws JsonParseException, JsonMappingException, IOException {
		SchemaCreateRequest request = new SchemaCreateRequest();

		Schema schema = new SchemaImpl();
		schema.setName("blogpost");
		schema.setDisplayField("name");

		assertEquals("name", schema.getDisplayField());
		assertEquals("blogpost", schema.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("string field name");
		schema.addField("name", stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean field name");
		booleanSchema.setValue(true);
		schema.addField("boolean", booleanSchema);

		ListFieldSchema<NodeField> listFieldSchema = new ListFieldSchemaImpl<>();
		listFieldSchema.setName("list field name");
		listFieldSchema.setLabel("list field label");
		listFieldSchema.setListType("node");
		listFieldSchema.setMin(5);
		listFieldSchema.setMax(10);
		listFieldSchema.setAllowedSchemas(new String[] { "image", "gallery" });
		NodeField defaultNode = new NodeFieldImpl();
		defaultNode.setUuid(UUIDUtil.randomUUID());
		listFieldSchema.getItems().add(defaultNode);
		schema.addField("list", listFieldSchema);

		//		MicroschemaFieldSchema microschemaFieldSchema = new MicroschemaFieldSchemaImpl();
		//		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "gallery", "shorttext" });
		//		microschemaFieldSchema.setLabel("gallery label");
		//		microschemaFieldSchema.setName("gallery name");
		//
		//		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		//		nodeFieldSchema.setUuid(UUIDUtil.randomUUID());
		//		nodeFieldSchema.setLabel("gallery node label");
		//		nodeFieldSchema.setName("gallery node name");
		//
		//		microschemaFieldSchema.getFields().add(nodeFieldSchema);
		//		request.addField(microschemaFieldSchema);

		request.setSchema(schema);
		schema = schemaStorage.getSchema(schema.getName());
		assertNotNull(schema);

		String json = JsonUtil.toJson(request);
		SchemaCreateRequest loadedRequest = JsonUtil.readValue(json, SchemaCreateRequest.class);
		assertNotNull(loadedRequest);
		String json2 = JsonUtil.toJson(loadedRequest);
		assertEquals(json, json2);
		System.out.println(json2);

	}
}
