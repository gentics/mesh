package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class MicroschemaProjectVerticleTest extends AbstractRestVerticleTest {

	@Test
	public void testReadProjectMicroschemas() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaListResponse list = call(() -> getClient().findMicroschemas(PROJECT_NAME));
			assertEquals(2, list.getData().size());

			call(() -> getClient().unassignMicroschemaFromProject(PROJECT_NAME, microschemaContainer("vcard").getUuid()));

			list = call(() -> getClient().findMicroschemas(PROJECT_NAME));
			assertEquals(1, list.getData().size());
		}
	}

	// Microschema Project Testcases - PUT / Add

	@Test
	public void testAddMicroschemaToExtraProject() {
		final String name = "test12345";
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchemaReference(new SchemaReference().setName("folder"));
			request.setName(name);

			ProjectResponse restProject = call(() -> getClient().createProject(request));

			call(() -> getClient().assignMicroschemaToProject(restProject.getName(), microschema.getUuid()));
		}
	}

	@Test
	public void testAddMicroschemaToProjectWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			ProjectRoot projectRoot = meshRoot().getProjectRoot();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchemaReference(new SchemaReference().setName("folder"));
			request.setName("extraProject");
			ProjectResponse created = call(() -> getClient().createProject(request));
			Project extraProject = projectRoot.findByUuid(created.getUuid());

			// Add only read perms
			role().grantPermissions(microschema, READ_PERM);
			role().grantPermissions(extraProject, UPDATE_PERM);

			Microschema restMicroschema = call(() -> getClient().assignMicroschemaToProject(extraProject.getName(), microschema.getUuid()));
			assertThat(restMicroschema.getUuid()).isEqualTo(microschema.getUuid());
			extraProject.reload();
			extraProject.getMicroschemaContainerRoot().reload();
			assertNotNull("The microschema should be added to the extra project",
					extraProject.getMicroschemaContainerRoot().findByUuid(microschema.getUuid()));
		}
	}

	@Test
	public void testAddMicroschemaToProjectWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Project project = project();
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			Project extraProject = projectRoot.create("extraProject", user(), schemaContainer("folder").getLatestVersion());
			// Add only read perms
			role().grantPermissions(microschema, READ_PERM);
			role().grantPermissions(project, READ_PERM);
			call(() -> getClient().assignMicroschemaToProject(extraProject.getName(), microschema.getUuid()), FORBIDDEN, "error_missing_perm",
					extraProject.getUuid());
			// Reload the schema and check for expected changes
			assertFalse("The microschema should not have been added to the extra project but it was",
					extraProject.getMicroschemaContainerRoot().contains(microschema));
		}
	}

	// Microschema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveMicroschemaFromProjectWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Project project = project();
			assertTrue("The microschema should be assigned to the project.", project.getMicroschemaContainerRoot().contains(microschema));

			call(() -> getClient().unassignMicroschemaFromProject(project.getName(), microschema.getUuid()));

			MicroschemaListResponse list = call(() -> getClient().findMicroschemas(PROJECT_NAME));

			assertEquals("The removed microschema should not be listed in the response", 0,
					list.getData().stream().filter(s -> s.getUuid().equals(microschema.getUuid())).count());
			project.getMicroschemaContainerRoot().reload();
			assertFalse("The microschema should no longer be assigned to the project.", project.getMicroschemaContainerRoot().contains(microschema));
		}
	}

	@Test
	public void testRemoveMicroschemaFromProjectWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainer("vcard");
			Project project = project();

			assertTrue("The microschema should be assigned to the project.", project.getMicroschemaContainerRoot().contains(microschema));
			// Revoke update perms on the project
			role().revokePermissions(project, UPDATE_PERM);

			call(() -> getClient().unassignMicroschemaFromProject(project.getName(), microschema.getUuid()), FORBIDDEN, "error_missing_perm",
					project.getUuid());

			// Reload the microschema and check for expected changes
			assertTrue("The microschema should still be listed for the project.", project.getMicroschemaContainerRoot().contains(microschema));
		}
	}

}
