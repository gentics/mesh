package com.gentics.mesh.core;

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

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	@Override
	public void testFindByName() throws IOException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
			assertNotNull(schemaContainer);
			assertEquals("content", schemaContainer.getSchema().getName());
			assertNull(meshRoot().getSchemaContainerRoot().findByName("content1235"));
		}
	}

	@Test
	@Override
	public void testRootNode() throws MeshSchemaException {
		try (Trx tx = new Trx(db)) {
			SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
			int nSchemasBefore = root.findAll().size();
			Schema schema = new SchemaImpl();
			schema.setName("test123");
			assertNotNull(root.create(schema, user()));
			int nSchemasAfter = root.findAll().size();
			assertEquals(nSchemasBefore + 1, nSchemasAfter);
		}
	}

	@Test
	public void testDefaultSchema() {
		try (Trx tx = new Trx(db)) {
			SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
			assertEquals(4, root.findAll().size());
		}
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
		try (Trx tx = new Trx(db)) {
			Page<? extends SchemaContainer> page = meshRoot().getSchemaContainerRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Trx tx = new Trx(db)) {
			List<? extends SchemaContainer> schemaContainers = meshRoot().getSchemaContainerRoot().findAll();
			assertNotNull(schemaContainers);
			assertEquals(4, schemaContainers.size());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
			String uuid = getSchemaContainer().getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getSchemaContainerRoot().findByUuid(uuid, rh -> {
				assertNotNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testDelete() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
			String uuid = getSchemaContainer().getUuid();
			getSchemaContainer().delete();

			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getSchemaContainerRoot().findByUuid(uuid, rh -> {
				assertNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer container = getSchemaContainer();
			Schema schema = container.getSchema();
			assertNotNull(schema);
			String json = JsonUtil.toJson(schema);
			assertNotNull(json);
			Schema deserializedSchema = JsonUtil.readSchema(json, SchemaImpl.class);
			assertNotNull(deserializedSchema);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws MeshSchemaException, InterruptedException {
		SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
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
		try (Trx tx = new Trx(db)) {
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
			assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, newContainer));
			getRequestUser().addCRUDPermissionOnRole(meshRoot().getSchemaContainerRoot(), GraphPermission.CREATE_PERM, newContainer);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new schema container.",
					role().hasPermission(GraphPermission.CREATE_PERM, newContainer));
		}

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
		try (Trx tx = new Trx(db)) {
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

	}

	@Test
	@Override
	public void testReadPermission() throws MeshSchemaException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
			testPermission(GraphPermission.READ_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshSchemaException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
			testPermission(GraphPermission.DELETE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshSchemaException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
			testPermission(GraphPermission.UPDATE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshSchemaException {
		try (Trx tx = new Trx(db)) {
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(new SchemaImpl(), user());
			testPermission(GraphPermission.CREATE_PERM, newContainer);
		}
	}

}
