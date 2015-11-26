package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		SchemaContainer schema = schemaContainer("folder");
		InternalActionContext ac = getMockedInternalActionContext("");
		SchemaReference reference = schema.transformToReference(ac);
		assertNotNull(reference);
		assertEquals(schema.getUuid(), reference.getUuid());
		assertEquals(schema.getName(), reference.getName());
	}

	@Test
	@Override
	public void testFindByName() throws IOException {
		SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
		assertNotNull(schemaContainer);
		assertEquals("content", schemaContainer.getSchema().getName());
		assertNull(meshRoot().getSchemaContainerRoot().findByName("content1235"));
	}

	@Test
	@Override
	public void testRootNode() throws MeshSchemaException {
		SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
		int nSchemasBefore = root.findAll().size();
		Schema schema = new SchemaImpl();
		schema.setName("test123");
		schema.setDisplayField("name");
		assertNotNull(root.create(schema, user()));
		int nSchemasAfter = root.findAll().size();
		assertEquals(nSchemasBefore + 1, nSchemasAfter);
	}

	@Test
	public void testDefaultSchema() {
		SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
		assertEquals(schemaContainers().size(), root.findAll().size());
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
		Page<? extends SchemaContainer> page = meshRoot().getSchemaContainerRoot().findAll(getRequestUser(), new PagingParameter(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends SchemaContainer> schemaContainers = meshRoot().getSchemaContainerRoot().findAll();
		assertNotNull(schemaContainers);
		assertEquals(4, schemaContainers.size());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		String uuid = getSchemaContainer().getUuid();
		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getSchemaContainerRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		String uuid = getSchemaContainer().getUuid();
		getSchemaContainer().delete();

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getSchemaContainerRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		SchemaContainer container = getSchemaContainer();
		Schema schema = container.getSchema();
		assertNotNull(schema);
		String json = JsonUtil.toJson(schema);
		assertNotNull(json);
		Schema deserializedSchema = JsonUtil.readSchema(json, SchemaImpl.class);
		assertNotNull(deserializedSchema);
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		assertNotNull(newContainer);
		String uuid = newContainer.getUuid();
		newContainer.delete();

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getSchemaContainerRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshSchemaException {
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, newContainer));
		getRequestUser().addCRUDPermissionOnRole(meshRoot().getSchemaContainerRoot(), GraphPermission.CREATE_PERM, newContainer);
		assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new schema container.",
				role().hasPermission(GraphPermission.CREATE_PERM, newContainer));

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
		SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
		Schema schema = schemaContainer.getSchema();
		schema.setName("changed");
		schemaContainer.setSchema(schema);
		assertEquals("changed", schemaContainer.getSchema().getName());
		schemaContainer.setName("changed2");
		assertEquals("changed2", schemaContainer.getName());

		schema = schemaContainer.getSchema();
		schema.setFolder(true);
		assertTrue(schema.isFolder());
		schemaContainer.setSchema(schema);
		schema = schemaContainer.getSchema();
		assertTrue(schema.isFolder());

		schema = schemaContainer.getSchema();
		schema.setFolder(false);
		assertFalse(schema.isFolder());
		schemaContainer.setSchema(schema);
		schema = schemaContainer.getSchema();
		assertFalse(schema.isFolder());
	}

	@Test
	@Override
	public void testReadPermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.READ_PERM, newContainer);
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.DELETE_PERM, newContainer);
	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.UPDATE_PERM, newContainer);
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaImpl();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.CREATE_PERM, newContainer);
	}

}
