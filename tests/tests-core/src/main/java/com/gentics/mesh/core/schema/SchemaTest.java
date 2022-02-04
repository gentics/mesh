package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.google.common.collect.Iterables;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibSchema schema = schemaContainer("folder");
			SchemaReference reference = schema.getLatestVersion().transformToReference();
			assertNotNull(reference);
			assertEquals(schema.getUuid(), reference.getUuid());
			assertEquals(schema.getLatestVersion().getName(), reference.getName());
			assertEquals(schema.getLatestVersion().getVersion(), reference.getVersion());
		}
	}

	@Test
	public void testGetContentFromSchemaVersion() {
		Bucket bucket = new Bucket(0, Integer.MAX_VALUE / 2, 0, 1);
		HibNodeFieldContainer content = tx(tx -> {
			return tx.contentDao().getLatestDraftFieldContainer(content(), "en");
		});
		HibSchemaVersion version = tx(() -> schemaContainer("content").getLatestVersion());

		long before = tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			content.setBucketId(Integer.MAX_VALUE);
			// Count contents in bucket 0 to MaxInt/2
			return schemaDao.getFieldContainers(version, initialBranchUuid(), bucket).count();
		});

		// Now alter the bucketId
		tx(tx -> {
			content.setBucketId(100);
		});

		tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			// Now set the bucketId so that the content is within the bounds of the bucket / range query
			long after = schemaDao.getFieldContainers(version, initialBranchUuid(), bucket).count();
			assertEquals("We should find one more content.", before + 1, after);
		});

		// Now set the bucketId on the end of the bucket to ensure we include the last element in the range
		tx(tx -> {
			content.setBucketId(bucket.end());
		});

		tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			// Now set the bucketId so that the content is within the bounds of the bucket / range query
			long after = schemaDao.getFieldContainers(version, initialBranchUuid(), bucket).count();
			assertEquals("We should still find the altered element ", before + 1, after);
		});

		// Now exceed the bucket
		tx(tx -> {
			content.setBucketId(bucket.end() + 1);
		});

		tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			// Now set the bucketId so that the content is within the bounds of the bucket / range query
			long after = schemaDao.getFieldContainers(version, initialBranchUuid(), bucket).count();
			assertEquals("We should still find the altered element ", before, after);
		});

	}

	@Test
	@Override
	public void testFindByName() throws IOException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			HibSchema schemaContainer = schemaDao.findByName("content");
			assertNotNull(schemaContainer);
			assertEquals("content", schemaContainer.getLatestVersion().getSchema().getName());
			assertNull(schemaDao.findByName("content1235"));
		}
	}

	@Test
	@Override
	public void testRootNode() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();

			long nSchemasBefore = schemaDao.count();
			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			assertNotNull(schemaDao.create(schema, user()));
			long nSchemasAfter = schemaDao.count();
			assertEquals(nSchemasBefore + 1, nSchemasAfter);
		}
	}

	@Test
	public void testDefaultSchema() {
		try (Tx tx = tx()) {
			assertEquals(schemaContainers().size(), tx.schemaDao().count());
		}
	}

	@Test
	public void testSchemaStorage() {
		try (Tx tx = tx()) {
			meshDagger().serverSchemaStorage().clear();
			meshDagger().serverSchemaStorage().init();
			SchemaModel schema = meshDagger().serverSchemaStorage().getSchema("folder");
			assertNotNull(schema);
			assertEquals("folder", schema.getName());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Page<? extends HibSchema> page = tx.schemaDao().findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			int size = Iterables.size(tx.schemaDao().findAll());
			assertEquals(schemaContainers().size(), size);
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			String uuid = getSchemaContainer().getUuid();
			assertNotNull("The schema could not be found", tx.schemaDao().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			BulkActionContext context = createBulkContext();
			String uuid = getSchemaContainer().getUuid();
			NodeDao nodeDao = tx.nodeDao();
			HibSchema schema = tx.schemaDao().findByUuid(uuid);
			for (HibNode node : schemaDao.getNodes(schema)) {
				nodeDao.delete(node, context, false, true);
			}
			schemaDao.delete(schema, context);
			assertNull("The schema should have been deleted", tx.schemaDao().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (Tx tx = tx()) {
			HibSchema container = getSchemaContainer();
			SchemaVersionModel schema = container.getLatestVersion().getSchema();
			assertNotNull(schema);
			String json = schema.toJson();
			assertNotNull(json);
			SchemaModel deserializedSchema = JsonUtil.readValue(json, SchemaModelImpl.class);
			assertNotNull(deserializedSchema);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();

			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			assertNotNull(newContainer);
			String uuid = newContainer.getUuid();
			schemaDao.delete(newContainer, createBulkContext());
			assertNull("The container should have been deleted", tx.schemaDao().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshSchemaException {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			SchemaDao schemaDao = tx.schemaDao();

			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			assertFalse(roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, newContainer));
			userDao.inheritRolePermissions(getRequestUser(), tx.data().permissionRoots().schema(), newContainer);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new schema container.",
				roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, newContainer));
		}

	}

	@Test
	@Override
	public void testRead() throws IOException {
		try (Tx tx = tx()) {
			assertNotNull(getSchemaContainer().getLatestVersion().getSchema());
		}
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		try (Tx tx = tx()) {
			assertNotNull(getSchemaContainer().getLatestVersion().getSchema());
			assertEquals("The schema container and schema rest model version must always be in sync", getSchemaContainer().getLatestVersion()
				.getVersion(), getSchemaContainer().getLatestVersion().getSchema().getVersion());
		}
	}

	@Test
	@Override
	public void testUpdate() throws IOException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			HibSchema schemaContainer = schemaDao.findByName("content");
			HibSchemaVersion currentVersion = schemaContainer.getLatestVersion();
			SchemaVersionModel schema = currentVersion.getSchema();
			schema.setName("changed");
			currentVersion.setSchema(schema);
			assertEquals("changed", currentVersion.getSchema().getName());
			currentVersion.setName("changed2");
			// Schema containers and schema versions have different names
			// TODO CL-348
			// assertEquals("changed2", schemaContainer.getName());

			schema = currentVersion.getSchema();
			schema.setContainer(true);
			assertTrue("The schema container flag should be set to true since we updated it.", schema.getContainer());
			currentVersion.setSchema(schema);
			schema = currentVersion.getSchema();
			assertTrue(schema.getContainer());

			schema = currentVersion.getSchema();
			schema.setContainer(false);
			assertFalse(schema.getContainer());
			currentVersion.setSchema(schema);
			schema = currentVersion.getSchema();
			assertFalse(schema.getContainer());
		}
	}

	@Test
	@Override
	public void testReadPermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			testPermission(InternalPermission.READ_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			testPermission(InternalPermission.DELETE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			testPermission(InternalPermission.UPDATE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			SchemaVersionModel schema = FieldUtil.createMinimalValidSchema();
			HibSchema newContainer = schemaDao.create(schema, user());
			testPermission(InternalPermission.CREATE_PERM, newContainer);
		}
	}

}
