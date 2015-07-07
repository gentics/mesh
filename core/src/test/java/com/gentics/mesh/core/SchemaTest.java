package com.gentics.mesh.core;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	private SchemaContainerRoot schemaContainerRoot;

	@Before
	public void setup() throws Exception {
		super.setup();
		schemaContainerRoot = boot.schemaContainerRoot();
	}

	@Test
	@Override
	public void testFindByName() throws IOException {
		SchemaContainer schemaContainer = schemaContainerRoot.findByName(PROJECT_NAME, "content");
		assertNotNull(schemaContainer);
		assertEquals("content", schemaContainer.getSchema().getName());
		assertNull(schemaContainerRoot.findByName(PROJECT_NAME, "content1235"));
	}

	@Test
	@Override
	public void testRootNode() {
		SchemaContainerRoot root = data().getMeshRoot().getSchemaContainerRoot();
		int nSchemasBefore = root.findAll().size();
		assertNotNull(root.create("test1235"));
		int nSchemasAfter = root.findAll().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}

	@Test
	public void testDefaultSchema() {
		SchemaContainerRoot root = data().getMeshRoot().getSchemaContainerRoot();
		assertEquals(4, root.findAll().size());
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
		Page<? extends SchemaContainer> page = schemaContainerRoot.findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends SchemaContainer> schemaContainers = schemaContainerRoot.findAll();
		assertNotNull(schemaContainers);
		assertEquals(4, schemaContainers.size());
	}

	@Test
	@Override
	public void testFindByUUID() {
		String uuid = getSchemaContainer().getUuid();
		assertNotNull(schemaContainerRoot.findByUUID(uuid));
	}

	@Test
	@Override
	public void testDelete() {
		String uuid = getSchemaContainer().getUuid();
		getSchemaContainer().delete();
		assertNull(schemaContainerRoot.findByUUID(uuid));
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		SchemaContainer container = getSchemaContainer();
		Schema schema = container.getSchema();
		assertNotNull(schema);
		String json = JsonUtil.toJson(schema);
		assertNotNull(json);
		Schema deserializedSchema = JsonUtil.readSchema(json);
		assertNotNull(deserializedSchema);
	}

	@Test
	@Override
	public void testCreateDelete() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		assertNotNull(newContainer);
		String uuid = newContainer.getUuid();
		newContainer.delete();
		assertNull(schemaContainerRoot.findByUUID(uuid));
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		assertFalse(getRole().hasPermission(Permission.CREATE_PERM, newContainer));
		getRequestUser().addCRUDPermissionOnRole(getMeshRoot().getSchemaContainerRoot(), Permission.CREATE_PERM, newContainer);
		assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new schema container.",
				getRole().hasPermission(Permission.CREATE_PERM, newContainer));

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
	public void testUpdate() throws IOException {
		SchemaContainer schemaContainer = schemaContainerRoot.findByName(PROJECT_NAME, "content");
		Schema schema = schemaContainer.getSchema();
		schema.setName("changed");
		schemaContainer.setSchema(schema);
		assertEquals("changed", schemaContainer.getSchema().getName());
		schemaContainer.setSchemaName("changed2");
		assertEquals("changed2", schemaContainer.getSchemaName());

		schema = schemaContainer.getSchema();
		schema.setContainer(true);
		assertTrue(schema.isContainer());
		schemaContainer.setSchema(schema);
		schema = schemaContainer.getSchema();
		assertTrue(schema.isContainer());

		schema = schemaContainer.getSchema();
		schema.setContainer(false);
		assertFalse(schema.isContainer());
		schemaContainer.setSchema(schema);
		schema = schemaContainer.getSchema();
		assertFalse(schema.isContainer());

	}

	@Test
	@Override
	public void testReadPermission() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		testPermission(Permission.READ_PERM, newContainer);
	}

	@Test
	@Override
	public void testDeletePermission() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		testPermission(Permission.DELETE_PERM, newContainer);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		testPermission(Permission.UPDATE_PERM, newContainer);
	}

	@Test
	@Override
	public void testCreatePermission() {
		SchemaContainer newContainer = getMeshRoot().getSchemaContainerRoot().create("newcontainer");
		testPermission(Permission.CREATE_PERM, newContainer);
	}

}
