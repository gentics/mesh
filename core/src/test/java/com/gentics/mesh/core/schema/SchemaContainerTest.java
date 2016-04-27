package com.gentics.mesh.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class SchemaContainerTest extends AbstractBasicObjectTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		SchemaContainer schema = schemaContainer("folder");
		SchemaReference reference = schema.getLatestVersion().transformToReference();
		assertNotNull(reference);
		assertEquals(schema.getUuid(), reference.getUuid());
		assertEquals(schema.getLatestVersion().getName(), reference.getName());
		assertEquals(schema.getLatestVersion().getVersion(), reference.getVersion().intValue());
	}

	@Test
	public void testGetRoot() {
		SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content").toBlocking().single();
		RootVertex<SchemaContainer> root = schemaContainer.getRoot();
		assertNotNull(root);
	}

	@Test
	@Override
	public void testFindByName() throws IOException {
		SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content").toBlocking().single();
		assertNotNull(schemaContainer);
		assertEquals("content", schemaContainer.getLatestVersion().getSchema().getName());
		assertNull(meshRoot().getSchemaContainerRoot().findByName("content1235").toBlocking().single());
	}

	@Test
	@Override
	public void testRootNode() throws MeshSchemaException {
		SchemaContainerRoot root = meshRoot().getSchemaContainerRoot();
		int nSchemasBefore = root.findAll().size();
		Schema schema = new SchemaModel();
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
		PageImpl<? extends SchemaContainer> page = meshRoot().getSchemaContainerRoot()
				.findAll(getMockedInternalActionContext(""), new PagingParameter(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends SchemaContainer> schemaContainers = meshRoot().getSchemaContainerRoot().findAll();
		assertNotNull(schemaContainers);
		assertEquals(schemaContainers().size(), schemaContainers.size());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		String uuid = getSchemaContainer().getUuid();
		assertNotNull("The schema could not be found", meshRoot().getSchemaContainerRoot().findByUuid(uuid).toBlocking().single());
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		SearchQueueBatch batch = createBatch();
		String uuid = getSchemaContainer().getUuid();
		for(Node node: getSchemaContainer().getNodes()) {
			node.delete(batch);
		}
		getSchemaContainer().delete(batch);
		assertNull("The schema should have been deleted", meshRoot().getSchemaContainerRoot().findByUuid(uuid).toBlocking().single());
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		SchemaContainer container = getSchemaContainer();
		Schema schema = container.getLatestVersion().getSchema();
		assertNotNull(schema);
		String json = JsonUtil.toJson(schema);
		assertNotNull(json);
		Schema deserializedSchema = JsonUtil.readValue(json, SchemaModel.class);
		assertNotNull(deserializedSchema);
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		Schema schema = new SchemaModel();
		schema.setDisplayField("name");
		SchemaContainer newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		assertNotNull(newContainer);
		String uuid = newContainer.getUuid();
		SearchQueueBatch batch = createBatch();
		newContainer.delete(batch);
		assertNull("The container should have been deleted", meshRoot().getSchemaContainerRoot().findByUuid(uuid).toBlocking().single());
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshSchemaException {
		Schema schema = new SchemaModel();
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
		assertNotNull(getSchemaContainer().getLatestVersion().getSchema());
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		assertNotNull(getSchemaContainer().getLatestVersion().getSchema());
		assertEquals("The schema container and schema rest model version must always be in sync",
				getSchemaContainer().getLatestVersion().getVersion(), getSchemaContainer().getLatestVersion().getSchema().getVersion());
	}

	@Test
	@Override
	public void testUpdate() throws IOException {
		SchemaContainer schemaContainer = meshRoot().getSchemaContainerRoot().findByName("content").toBlocking().single();
		SchemaContainerVersion currentVersion = schemaContainer.getLatestVersion();
		Schema schema = currentVersion.getSchema();
		schema.setName("changed");
		currentVersion.setSchema(schema);
		assertEquals("changed", currentVersion.getSchema().getName());
		currentVersion.setName("changed2");
		// Schema containers and schema versions have different names
		//TODO CL-348
		//assertEquals("changed2", schemaContainer.getName());

		schema = currentVersion.getSchema();
		schema.setContainer(true);
		assertTrue("The schema container flag should be set to true since we updated it.", schema.isContainer());
		currentVersion.setSchema(schema);
		schema = currentVersion.getSchema();
		assertTrue(schema.isContainer());

		schema = currentVersion.getSchema();
		schema.setContainer(false);
		assertFalse(schema.isContainer());
		currentVersion.setSchema(schema);
		schema = currentVersion.getSchema();
		assertFalse(schema.isContainer());
	}

	@Test
	@Override
	public void testReadPermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaModel();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.READ_PERM, newContainer);
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaModel();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.DELETE_PERM, newContainer);
	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaModel();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.UPDATE_PERM, newContainer);
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshSchemaException {
		SchemaContainer newContainer;
		Schema schema = new SchemaModel();
		schema.setDisplayField("name");
		newContainer = meshRoot().getSchemaContainerRoot().create(schema, user());
		testPermission(GraphPermission.CREATE_PERM, newContainer);
	}

}
