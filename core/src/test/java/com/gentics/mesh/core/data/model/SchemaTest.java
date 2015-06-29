package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.request.SchemaCreateRequest;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.JsonUtils;

public class SchemaTest extends AbstractDBTest {

	@Autowired
	private SchemaService schemaService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testFindByName() {
		Schema schema = schemaService.findByName(PROJECT_NAME, "content");
		assertNotNull(schema);
		assertEquals("content", schema.getName());
		assertNull(schemaService.findByName(PROJECT_NAME, "content1235"));
	}

	@Test
	public void testDeleteByObject() {
		Schema schema = data().getSchema("content");
		String uuid = schema.getUuid();
		schema.delete();
		assertNull(schemaService.findByUUID(uuid));
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
		SchemaRoot root = data().getMeshRoot().getSchemaRoot();
		int nSchemasBefore = root.getSchemas().size();
		assertNotNull(root.create("test1235"));
		int nSchemasAfter = root.getSchemas().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}

	@Test
	public void testNodeSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("blogpost");
		request.setDisplayField("name");
		request.setUuid("bogusUUID");

		assertEquals("bogusUUID", request.getUuid());
		assertEquals("name", request.getDisplayField());
		assertEquals("blogpost", request.getName());

		StringFieldSchema stringSchema = new StringFieldSchemaImpl();
		stringSchema.setLabel("string field label");
		stringSchema.setName("string field name");
		request.addField(stringSchema);

		BooleanFieldSchema booleanSchema = new BooleanFieldSchemaImpl();
		booleanSchema.setLabel("boolean field label");
		booleanSchema.setName("boolean field name");
		booleanSchema.setValue(true);
		request.addField(booleanSchema);

		for (FieldSchema field : request.getFields()) {
			System.out.println(field.getName());
			System.out.println(field.getLabel());
			System.out.println(field.getType());
			System.out.println();
		}

		String json = JsonUtils.toJson(request);
		System.out.println(json);
	}
}
