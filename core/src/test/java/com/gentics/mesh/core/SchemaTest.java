package com.gentics.mesh.core;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.SchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.test.AbstractDBTest;

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
		// assertEquals("content", schema.getName());
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

}
