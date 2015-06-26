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
import com.gentics.mesh.test.AbstractDBTest;

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
		schemaService.delete(schema);
		assertNull(schemaService.findOne(schema.getId()));
	}

	@Test
	public void testDeleteByUUID() {
		Schema schema = data().getSchema("content");
		schemaService.deleteByUUID(schema.getUuid());
		assertNull(schemaService.findOne(schema.getId()));
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
}
