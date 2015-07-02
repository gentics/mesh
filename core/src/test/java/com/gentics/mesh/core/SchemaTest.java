package com.gentics.mesh.core;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.SchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaTest extends AbstractBasicObjectTest {

	@Autowired
	private SchemaContainerService schemaContainerService;

	@Autowired
	private SchemaStorage schemaStorage;

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
		List<? extends SchemaContainer> schemaContainers = schemaContainerService.findAll();
		assertNotNull(schemaContainers);
		assertEquals(4, schemaContainers.size());
	}

	@Test
	@Override
	public void testFindByUUID() {
		String uuid = getSchemaContainer().getUuid();
		assertNull(schemaContainerService.findByUUID(uuid));
	}

	@Test
	@Override
	public void testDelete() {
		String uuid = getSchemaContainer().getUuid();
		getSchemaContainer().delete();
		assertNull(schemaContainerService.findByUUID(uuid));

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
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreateDelete() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRead() throws IOException {
		assertNotNull(getSchemaContainer().getSchema());

	}

	@Test
	@Override
	public void testCreate() throws IOException {
		assertNotNull(getSchemaContainer().getSchema());
	}

	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDeletePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdatePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreatePermission() {
		fail("Not yet implemented");
	}

}
