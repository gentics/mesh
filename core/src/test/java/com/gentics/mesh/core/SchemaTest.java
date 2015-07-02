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
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.SchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaTest extends AbstractDBTest implements BasicObjectTestcases {

	@Autowired
	private SchemaContainerService schemaContainerService;

	@Autowired
	private SchemaStorage schemaStorage;

	private SchemaContainer schemaContainer;

	@Before
	public void setup() throws Exception {
		setupData();
		data().getSchemaContainer("content");
	}

	@Test
	@Override
	public void testFindByName() {
		SchemaContainer schema = schemaContainerService.findByName(PROJECT_NAME, "content");
		assertNotNull(schema);
		// assertEquals("content", schema.getName());
		assertNull(schemaContainerService.findByName(PROJECT_NAME, "content1235"));
	}

	@Test
	@Override
	public void testRootNode() {
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
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindByUUID() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDelete() {
		String uuid = schemaContainer.getUuid();
		schemaContainer.delete();
		assertNull(schemaContainerService.findByUUID(uuid));

		// UserInfo info = data().getUserInfo();
		// ObjectSchema schema = data().getContentSchema();
		// try (Transaction tx = graphDb.beginTx()) {
		// roleService.revokePermission(info.getRole(), schema, PermissionType.DELETE);
		// objectSchemaService.deleteByUUID(schema.getUuid());
		// tx.success();
		// }
		// assertNotNull(objectSchemaService.findOne(schema.getId()));
	}

	@Test
	@Override
	public void testTransformation() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}

}
