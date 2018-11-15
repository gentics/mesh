package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
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
			SchemaContainer schema = schemaContainer("content");

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			request.setName(name);

			ProjectResponse restProject = call(() -> client().createProject(request));

			call(() -> client().assignSchemaToProject(restProject.getName(), schema.getUuid()));
		}
	}

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		Project extraProject;
		SchemaContainer schema;
		try (Tx tx = tx()) {
			schema = schemaContainer("content");
			ProjectRoot projectRoot = meshRoot().getProjectRoot();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("extraProject");
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse created = call(() -> client().createProject(request));
			extraProject = projectRoot.findByUuid(created.getUuid());

			// Add only read perms
			role().grantPermissions(schema, READ_PERM);
			role().grantPermissions(extraProject, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().assignSchemaToProject(extraProject.getName(), schema.getUuid()));
			// assertThat(restSchema).matches(schema);
			assertNotNull("The schema should be added to the extra project", extraProject.getSchemaContainerRoot().findByUuid(schema.getUuid()));
		}
	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		String projectUuid;
		String schemaUuid;
		Project extraProject;
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			schemaUuid = schema.getUuid();
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("extraProject");
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse response = call(() -> client().createProject(request));
			projectUuid = response.getUuid();
			extraProject = projectRoot.findByUuid(projectUuid);
			// Revoke Update perm on project
			role().revokePermissions(extraProject, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().assignSchemaToProject("extraProject", schemaUuid), FORBIDDEN, "error_missing_perm", projectUuid, UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			// Reload the schema and check for expected changes
			SchemaContainer schema = schemaContainer("content");
			assertFalse("The schema should not have been added to the extra project but it was",
					extraProject.getSchemaContainerRoot().contains(schema));
		}

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			Project project = project();
			assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));

			call(() -> client().unassignSchemaFromProject(project.getName(), schema.getUuid()));

			SchemaListResponse list = call(() -> client().findSchemas(PROJECT_NAME));

			// final String removedProjectName = project.getName();
			assertEquals("The removed schema should not be listed in the response", 0,
					list.getData().stream().filter(s -> s.getUuid().equals(schema.getUuid())).count());
			assertFalse("The schema should no longer be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
		}
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		try (Tx tx = tx()) {
			assertTrue("The schema should be assigned to the project.", project().getSchemaContainerRoot().contains(schema));
			// Revoke update perms on the project
			role().revokePermissions(project(), UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().unassignSchemaFromProject(PROJECT_NAME, schema.getUuid()), FORBIDDEN, "error_missing_perm", projectUuid(), UPDATE_PERM.getRestPerm().getName());
			// Reload the schema and check for expected changes
			assertTrue("The schema should still be listed for the project.", project().getSchemaContainerRoot().contains(schema));
		}
	}

}
