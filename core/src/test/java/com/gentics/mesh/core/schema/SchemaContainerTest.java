package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.google.common.collect.Iterables;

@MeshTestSetting(testSize = FULL, startServer = false)
public class SchemaContainerTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("folder");
			SchemaReference reference = schema.getLatestVersion().transformToReference();
			assertNotNull(reference);
			assertEquals(schema.getUuid(), reference.getUuid());
			assertEquals(schema.getLatestVersion().getName(), reference.getName());
			assertEquals(schema.getLatestVersion().getVersion(), reference.getVersion());
		}
	}

	@Test
	public void testGetRoot() {
		try (Tx tx = tx()) {
			SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
			RootVertex<SchemaContainer> root = schemaContainer.getRoot();
			assertNotNull(root);
		}
	}

	@Test
	@Override
	public void testFindByName() throws IOException {
		try (Tx tx = tx()) {
			SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
			assertNotNull(schemaContainer);
			assertEquals("content", schemaContainer.getLatestVersion().getSchema().getName());
			assertNull(meshRoot().getSchemaContainerRoot().findByName("content1235"));
		}
	}

	@Test
	@Override
	public void testRootNode() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
			long nSchemasBefore = root.computeCount();
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			assertNotNull(root.create(schema, user()));
			long nSchemasAfter = root.computeCount();
			assertEquals(nSchemasBefore + 1, nSchemasAfter);
		}
	}

	@Test
	public void testDefaultSchema() {
		try (Tx tx = tx()) {
			SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
			assertEquals(schemaContainers().size(), root.computeCount());
		}
	}

	@Test
	public void testSchemaStorage() {
		try (Tx tx = tx()) {
			meshDagger().serverSchemaStorage().clear();
			meshDagger().serverSchemaStorage().init();
			Schema schema = meshDagger().serverSchemaStorage().getSchema("folder");
			assertNotNull(schema);
			assertEquals("folder", schema.getName());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Page<? extends SchemaContainer> page = meshRoot().getSchemaContainerRoot().findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			int size = Iterables.size(meshRoot().getSchemaContainerRoot().findAll());
			assertEquals(schemaContainers().size(), size);
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			String uuid = getSchemaContainer().getUuid();
			assertNotNull("The schema could not be found", meshRoot().getSchemaContainerRoot().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			BulkActionContext context = createBulkContext();
			String uuid = getSchemaContainer().getUuid();
			for (Node node : getSchemaContainer().getNodes()) {
				node.delete(context);
			}
			getSchemaContainer().delete(context);
			assertNull("The schema should have been deleted", meshRoot().getSchemaContainerRoot().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		try (Tx tx = tx()) {
			SchemaContainer container = getSchemaContainer();
			SchemaModel schema = container.getLatestVersion().getSchema();
			assertNotNull(schema);
			String json = schema.toJson();
			assertNotNull(json);
			Schema deserializedSchema = JsonUtil.readValue(json, SchemaModelImpl.class);
			assertNotNull(deserializedSchema);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			assertNotNull(newContainer);
			String uuid = newContainer.getUuid();
			newContainer.delete(createBulkContext());
			assertNull("The container should have been deleted", meshRoot().getSchemaContainerRoot().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, newContainer));
			getRequestUser().inheritRolePermissions(meshRoot().getSchemaContainerRoot(), newContainer);
			assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new schema container.", role().hasPermission(
				GraphPermission.CREATE_PERM, newContainer));
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
			SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content");
			SchemaContainerVersion currentVersion = schemaContainer.getLatestVersion();
			SchemaModel schema = currentVersion.getSchema();
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
			SchemaContainer newContainer;
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			testPermission(GraphPermission.READ_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaContainer newContainer;
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			testPermission(GraphPermission.DELETE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaContainer newContainer;
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			testPermission(GraphPermission.UPDATE_PERM, newContainer);
		}
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshSchemaException {
		try (Tx tx = tx()) {
			SchemaContainer newContainer;
			SchemaModel schema = FieldUtil.createMinimalValidSchema();
			newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
			testPermission(GraphPermission.CREATE_PERM, newContainer);
		}
	}

}
