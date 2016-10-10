package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SchemaProjectVerticleTest extends AbstractRestVerticleTest {

	@Test
	public void testReadProjectSchemas() {
		try (NoTx noTx = db.noTx()) {
			SchemaListResponse list = call(() -> getClient().findSchemas(PROJECT_NAME));
			assertEquals(3, list.getData().size());

			call(() -> getClient().unassignSchemaFromProject(PROJECT_NAME, schemaContainer("folder").getUuid()));

			list = call(() -> getClient().findSchemas(PROJECT_NAME));
			assertEquals(2, list.getData().size());
		}
	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToExtraProject() {
		final String name = "test12345";
		try (NoTx noTx = db.noTx()) {
			SchemaContainer schema = schemaContainer("content");

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchemaReference(new SchemaReference().setName("folder"));
			request.setName(name);

			ProjectResponse restProject = call(() -> getClient().createProject(request));

			call(() -> getClient().assignSchemaToProject(restProject.getName(), schema.getUuid()));
		}
	}

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			SchemaContainer schema = schemaContainer("content");
			ProjectRoot projectRoot = meshRoot().getProjectRoot();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("extraProject");
			request.setSchemaReference(new SchemaReference().setName("folder"));
			ProjectResponse created = call(() -> getClient().createProject(request));
			Project extraProject = projectRoot.findByUuid(created.getUuid());

			// Add only read perms
			role().grantPermissions(schema, READ_PERM);
			role().grantPermissions(extraProject, UPDATE_PERM);

			Schema restSchema = call(() -> getClient().assignSchemaToProject(extraProject.getName(), schema.getUuid()));
			assertThat(restSchema).matches(schema);
			extraProject.getSchemaContainerRoot().reload();
			assertNotNull("The schema should be added to the extra project", extraProject.getSchemaContainerRoot().findByUuid(schema.getUuid()));
		}
	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		String projectUuid;
		String schemaUuid;
		Project extraProject;
		try (NoTx noTx = db.noTx()) {
			SchemaContainer schema = schemaContainer("content");
			Project project = project();
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("extraProject");
			request.setSchemaReference(new SchemaReference().setName("folder"));
			ProjectResponse response = call(() -> getClient().createProject(request));
			projectUuid = response.getUuid();
			extraProject = projectRoot.findByUuid(projectUuid);
			schemaUuid = schema.getUuid();
			// Revoke Update perm on project
			role().revokePermissions(project, UPDATE_PERM);
		}
		call(() -> getClient().assignSchemaToProject("extraProject", schemaUuid), FORBIDDEN, "error_missing_perm", projectUuid);
		try (NoTx noTx = db.noTx()) {
			// Reload the schema and check for expected changes
			SchemaContainer schema = schemaContainer("content");
			assertFalse("The schema should not have been added to the extra project but it was",
					extraProject.getSchemaContainerRoot().contains(schema));
		}

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			SchemaContainer schema = schemaContainer("content");
			Project project = project();
			assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));

			call(() -> getClient().unassignSchemaFromProject(project.getName(), schema.getUuid()));

			SchemaListResponse list = call(() -> getClient().findSchemas(PROJECT_NAME));

			// final String removedProjectName = project.getName();
			assertEquals("The removed schema should not be listed in the response", 0,
					list.getData().stream().filter(s -> s.getUuid().equals(schema.getUuid())).count());
			project.getSchemaContainerRoot().reload();
			assertFalse("The schema should no longer be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
		}
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			SchemaContainer schema = schemaContainer("content");
			Project project = project();

			assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
			// Revoke update perms on the project
			role().revokePermissions(project, UPDATE_PERM);

			call(() -> getClient().unassignSchemaFromProject(project.getName(), schema.getUuid()), FORBIDDEN, "error_missing_perm",
					project.getUuid());

			// Reload the schema and check for expected changes
			assertTrue("The schema should still be listed for the project.", project.getSchemaContainerRoot().contains(schema));
		}
	}

}
