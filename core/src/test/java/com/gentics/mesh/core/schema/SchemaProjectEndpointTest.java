package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaProjectEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadProjectSchemas() {
		try (Tx tx = tx()) {
			SchemaListResponse list = call(() -> client().findSchemas(PROJECT_NAME));
			assertEquals(3, list.getData().size());

			call(() -> client().unassignSchemaFromProject(PROJECT_NAME, schemaContainer("folder").getUuid()));

			list = call(() -> client().findSchemas(PROJECT_NAME));
			assertEquals(2, list.getData().size());
		}
	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToExtraProject() {
		final String name = "test12345";
		try (Tx tx = tx()) {
			Schema schema = schemaContainer("content");

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			request.setName(name);

			ProjectResponse restProject = call(() -> client().createProject(request));

			call(() -> client().assignSchemaToProject(restProject.getName(), schema.getUuid()));
		}
	}

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("extraProject");
		request.setSchemaRef("folder");

		ProjectResponse created = call(() -> client().createProject(request));
		String projectUuid = created.getUuid();
		String projectName = created.getName();

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			ProjectDaoWrapper projectDao = tx.data().projectDao();

			Project extraProject = projectDao.findByUuid(created.getUuid());
			// Add only read perms
			Schema schema = schemaContainer("content");
			roleDao.grantPermissions(role(), schema, READ_PERM);
			roleDao.grantPermissions(role(), extraProject, UPDATE_PERM);
			tx.success();
		}

		expect(PROJECT_SCHEMA_ASSIGNED).match(1, ProjectSchemaEventModel.class, event -> {
			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(projectName, projectRef.getName());
			assertEquals(projectUuid, projectRef.getUuid());

			SchemaReference schemaRef = event.getSchema();
			assertNotNull(schemaRef);
			assertEquals("content", schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());
		});

		call(() -> client().assignSchemaToProject(projectName, schemaUuid));
		awaitEvents();

		try (Tx tx = tx()) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			Project extraProject = projectRoot.findByUuid(created.getUuid());
			assertNotNull("The schema should be added to the extra project", extraProject.getSchemaContainerRoot().findByUuid(schemaUuid));
		}
	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("extraProject");
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		ProjectResponse response = call(() -> client().createProject(request));
		String projectUuid = response.getUuid();

		Project extraProject = tx((tx) -> {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			ProjectDaoWrapper projectDao = tx.data().projectDao();
			// Revoke Update perm on project
			Project p = projectDao.findByUuid(projectUuid);
			roleDao.revokePermissions(role(), p, UPDATE_PERM);
			return p;
		});

		call(() -> client().assignSchemaToProject("extraProject", schemaUuid), FORBIDDEN, "error_missing_perm", projectUuid,
			UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			// Reload the schema and check for expected changes
			Schema schema = schemaContainer("content");
			assertFalse("The schema should not have been added to the extra project but it was",
				extraProject.getSchemaContainerRoot().contains(schema));
		}

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		Project project = project();
		Schema schema = schemaContainer("content");
		String schemaUuid = tx(() -> schema.getUuid());

		try (Tx tx = tx()) {
			assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
		}

		expect(PROJECT_SCHEMA_UNASSIGNED).match(1, ProjectSchemaEventModel.class, event -> {
			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());

			SchemaReference schemaRef = event.getSchema();
			assertNotNull(schemaRef);
			assertEquals("content", schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());
		});

		call(() -> client().unassignSchemaFromProject(PROJECT_NAME, schemaUuid));
		// TODO test for idempotency
		awaitEvents();

		SchemaListResponse list = call(() -> client().findSchemas(PROJECT_NAME));
		assertEquals("The removed schema should not be listed in the response", 0,
			list.getData().stream().filter(s -> s.getUuid().equals(schemaUuid)).count());

		try (Tx tx = tx()) {
			assertFalse("The schema should no longer be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
		}
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		Schema schema = schemaContainer("content");
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			assertTrue("The schema should be assigned to the project.", project().getSchemaContainerRoot().contains(schema));
			// Revoke update perms on the project
			roleDao.revokePermissions(role(), project(), UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().unassignSchemaFromProject(PROJECT_NAME, schema.getUuid()), FORBIDDEN, "error_missing_perm", projectUuid(),
				UPDATE_PERM.getRestPerm().getName());
			// Reload the schema and check for expected changes
			assertTrue("The schema should still be listed for the project.", project().getSchemaContainerRoot().contains(schema));
		}
	}

}
