package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MicroschemaProjectEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadProjectMicroschemas() {
		try (Tx tx = tx()) {
			MicroschemaListResponse list = call(() -> client().findMicroschemas(PROJECT_NAME));
			assertEquals(2, list.getData().size());

			call(() -> client().unassignMicroschemaFromProject(PROJECT_NAME, microschemaContainer("vcard").getUuid()));

			list = call(() -> client().findMicroschemas(PROJECT_NAME));
			assertEquals(1, list.getData().size());
		}
	}

	// Microschema Project Testcases - PUT / Add

	@Test
	public void testAddMicroschemaToExtraProject() {
		final String name = "test12345";
		final String uuid = tx(() -> microschemaContainer("vcard").getUuid());

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		request.setName(name);
		ProjectResponse restProject = call(() -> client().createProject(request));

		expect(PROJECT_MICROSCHEMA_ASSIGNED).match(1, ProjectMicroschemaEventModel.class, event -> {
			MicroschemaReference microschemaRef = event.getMicroschema();
			assertNotNull(microschemaRef);
			assertEquals("vcard", microschemaRef.getName());
			assertEquals(uuid, microschemaRef.getUuid());
			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(restProject.getName(), projectRef.getName());
			assertEquals(restProject.getUuid(), projectRef.getUuid());
		});
		call(() -> client().assignMicroschemaToProject(restProject.getName(), uuid));
		awaitEvents();
	}

	@Test
	public void testAddMicroschemaToProjectWithPerm() throws Exception {
		HibProject extraProject;
		HibMicroschema microschema = microschemaContainer("vcard");

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			ProjectRoot projectRoot = meshRoot().getProjectRoot();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			request.setName("extraProject");
			ProjectResponse created = call(() -> client().createProject(request));
			extraProject = projectRoot.findByUuid(created.getUuid());

			// Add only read perms
			roleDao.grantPermissions(role(), microschema, READ_PERM);
			roleDao.grantPermissions(role(), extraProject, UPDATE_PERM);
			tx.success();
		}
		try (Tx tx = tx()) {
			MicroschemaDao microschemaDao = tx.microschemaDao();
			MicroschemaResponse restMicroschema = call(() -> client().assignMicroschemaToProject(extraProject.getName(), microschema.getUuid()));
			assertThat(restMicroschema.getUuid()).isEqualTo(microschema.getUuid());
			assertNotNull("The microschema should be added to the extra project",
				microschemaDao.findByUuid(extraProject, microschema.getUuid()));
		}
	}

	@Test
	public void testAddMicroschemaToProjectWithoutPerm() throws Exception {
		String projectUuid;
		String microschemaUuid;
		HibProject extraProject;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibMicroschema microschema = microschemaContainer("vcard");
			microschemaUuid = microschema.getUuid();
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("extraProject");
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse response = call(() -> client().createProject(request));
			projectUuid = response.getUuid();
			extraProject = projectRoot.findByUuid(projectUuid);
			// Revoke Update perm on project
			roleDao.revokePermissions(role(), extraProject, UPDATE_PERM);
			tx.success();
		}

		call(() -> client().assignMicroschemaToProject("extraProject", microschemaUuid), FORBIDDEN, "error_missing_perm", projectUuid,
			UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			MicroschemaDao microschemaDao = tx.microschemaDao();
			// Reload the microschema and check for expected changes
			HibMicroschema microschema = microschemaContainer("vcard");

			assertFalse("The microschema should not have been added to the extra project but it was",
				microschemaDao.contains(extraProject, microschema));
		}
	}

	// Microschema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveMicroschemaFromProjectWithPerm() throws Exception {
		HibProject project = project();
		HibMicroschema microschema = microschemaContainer("vcard");
		String microschemaUuid = tx(() -> microschema.getUuid());
		String microschemaName = "vcard";

		try (Tx tx = tx()) {
			assertTrue("The microschema should be assigned to the project.", tx.microschemaDao().contains(project, microschema));
		}

		expect(PROJECT_MICROSCHEMA_UNASSIGNED).match(1, ProjectMicroschemaEventModel.class, event -> {
			MicroschemaReference microschemaRef = event.getMicroschema();
			assertNotNull(microschemaRef);
			assertEquals(microschemaName, microschemaRef.getName());
			assertEquals(microschemaUuid, microschemaRef.getUuid());
			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());
		});
		call(() -> client().unassignMicroschemaFromProject(PROJECT_NAME, microschemaUuid));
		awaitEvents();

		MicroschemaListResponse list = call(() -> client().findMicroschemas(PROJECT_NAME));

		try (Tx tx = tx()) {
			assertEquals("The removed microschema should not be listed in the response", 0,
				list.getData().stream().filter(s -> s.getUuid().equals(microschemaUuid)).count());
			assertFalse("The microschema should no longer be assigned to the project.",
				tx.microschemaDao().contains(project(), microschema));
		}
	}

	@Test
	public void testRemoveMicroschemaFromProjectWithoutPerm() throws Exception {
		HibProject project = project();
		HibMicroschema microschema = microschemaContainer("vcard");

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertTrue("The microschema should be assigned to the project.", tx.microschemaDao().contains(project, microschema));
			// Revoke update perms on the project
			roleDao.revokePermissions(role(), project, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().unassignMicroschemaFromProject(project.getName(), microschema.getUuid()), FORBIDDEN, "error_missing_perm",
				project.getUuid(), UPDATE_PERM.getRestPerm().getName());

			// Reload the microschema and check for expected changes
			assertTrue("The microschema should still be listed for the project.", tx.microschemaDao().contains(project, microschema));
		}
	}

}
