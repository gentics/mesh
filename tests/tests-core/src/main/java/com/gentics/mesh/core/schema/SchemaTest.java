package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
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
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.google.common.collect.Iterables;

@MeshTestSetting(testSize = FULL, startServer = true)
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

		tx(tx -> {
			content.setBucketId(Integer.MAX_VALUE);
		});

		long before = tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
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

	/**
	 * Test implementation of {@link SchemaDao#findNodes(HibSchemaVersion, String, HibUser, ContainerType)}.
	 * Check whether
	 * <ol>
	 * <li>Project base node is returned</li>
	 * <li>All nodes of the branch are returned</li>
	 * <li>Nodes of other branches are not returned</li>
	 * <li>User permissions are checked</li>
	 * </ol>
	 */
	@Test
	public void testFindNodes() {
		String projectName = tx(() -> project().getName());
		String initialBranchUuid = tx(() -> project().getInitialBranch().getUuid());
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		Set<String> folders = tx(() -> data().getFolders().values().stream()
				.map(node -> getDisplayName(node, initialBranchUuid)).collect(Collectors.toSet()));
		folders.add(tx(() -> getDisplayName(project().getBaseNode(), initialBranchUuid)));

		// revoke permission to read folder("news")
		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			HibNode newsFolder = data().getFolder("news");
			roleDao.revokePermissions(role(), newsFolder, InternalPermission.READ_PERM);
			return newsFolder.getUuid();
		});

		// revoke permission to read published  folder("deals")
		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			HibNode newsFolder = data().getFolder("deals");
			roleDao.revokePermissions(role(), newsFolder, InternalPermission.READ_PUBLISHED_PERM);
			return newsFolder.getUuid();
		});

		// revoke all read permission from folder("products")
		String product = tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			HibNode newsFolder = data().getFolder("products");
			roleDao.revokePermissions(role(), newsFolder, InternalPermission.READ_PERM);
			roleDao.revokePermissions(role(), newsFolder, InternalPermission.READ_PUBLISHED_PERM);
			return getDisplayName(newsFolder, initialBranchUuid);
		});
		folders.remove(product);

		String newBranchUuid = tx(() -> {
			HibBranch newBranch = createBranch("newbranch");

			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(newBranch.getPreviousBranch());
			meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

			return newBranch.getUuid();
		});

		// create new folder in initial branch
		String initialDraftFolder = call(() -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest()
					.setParentNode(new NodeReference().setUuid(baseNodeUuid))
					.setSchema(new SchemaReferenceImpl().setName("folder"))
					.setLanguage("en");
			nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("in initial branch"));
			return client().createNode(projectName, nodeCreateRequest, new VersioningParametersImpl().setBranch(initialBranchUuid));
		}).getDisplayName();

		// create new folder in new branch
		String newDraftFolder = call(() -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest()
					.setParentNode(new NodeReference().setUuid(baseNodeUuid))
					.setSchema(new SchemaReferenceImpl().setName("folder")).setLanguage("en");
			nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("in new branch"));
			return client().createNode(projectName, nodeCreateRequest, new VersioningParametersImpl().setBranch(newBranchUuid));
		}).getDisplayName();

		for (Triple<String, ContainerType, String> testCase : Arrays.asList(
				Triple.of(initialBranchUuid, ContainerType.DRAFT, initialDraftFolder),
				Triple.of(newBranchUuid, ContainerType.DRAFT, newDraftFolder),
				Triple.of(initialBranchUuid, ContainerType.PUBLISHED, (String)null),
				Triple.of(newBranchUuid, ContainerType.PUBLISHED, (String)null))) {
			Set<String> expected = new HashSet<>(folders);
			if (testCase.getRight() != null) {
				expected.add(testCase.getRight());
			}

			try (Tx tx = tx()) {
				SchemaDao schemaDao = tx.schemaDao();
				HibUser user = user();
				HibSchemaVersion folderSchema = schemaContainer("folder").getLatestVersion();
				List<? extends HibNode> nodes = schemaDao.findNodes(folderSchema, testCase.getLeft(), user, testCase.getMiddle()).list();
				List<String> nodeUuids = nodes.stream().map(node -> getDisplayName(node, testCase.getLeft())).collect(Collectors.toList());
				assertThat(nodeUuids).doesNotHaveDuplicates().containsOnlyElementsOf(expected);
			}
		}
	}
}
