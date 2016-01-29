package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class SchemaProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private ProjectSchemaVerticle projectSchemaVerticle;

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(schemaVerticle);
		list.add(projectSchemaVerticle);
		list.add(projectVerticle);
		return list;
	}

	@Test
	public void testReadProjectSchemas() {
		Future<SchemaListResponse> future = getClient().findSchemas(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);
		SchemaListResponse list = future.result();
		assertEquals(3, list.getData().size());

		Future<SchemaResponse> removeFuture = getClient().removeSchemaFromProject(schemaContainer("folder").getUuid(), project().getUuid());
		latchFor(removeFuture);
		assertSuccess(removeFuture);

		future = getClient().findSchemas(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);
		list = future.result();
		assertEquals(2, list.getData().size());
	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToExtraProject() {
		final String name = "test12345";
		SchemaContainer schema = schemaContainer("content");

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		Future<ProjectResponse> future = getClient().createProject(request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();

		Future<SchemaResponse> addSchemaFuture = getClient().addSchemaToProject(schema.getUuid(), restProject.getUuid());
		latchFor(addSchemaFuture);
		assertSuccess(addSchemaFuture);
	}

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject", user());

		// Add only read perms
		role().grantPermissions(schema, READ_PERM);
		role().grantPermissions(extraProject, UPDATE_PERM);

		Future<SchemaResponse> future = getClient().addSchemaToProject(schema.getUuid(), extraProject.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		assertThat(restSchema).matches(schema);
		extraProject.getSchemaContainerRoot().reload();
		assertNotNull("The schema should be added to the extra project", extraProject.getSchemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().single());
	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject", user());
		// Add only read perms
		role().grantPermissions(schema, READ_PERM);
		role().grantPermissions(project, READ_PERM);
		Future<SchemaResponse> future = getClient().addSchemaToProject(schema.getUuid(), extraProject.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraProject.getUuid());
		// Reload the schema and check for expected changes
		assertFalse("The schema should not have been added to the extra project but it was", extraProject.getSchemaContainerRoot().contains(schema));

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();
		assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));

		Future<SchemaResponse> future = getClient().removeSchemaFromProject(schema.getUuid(), project.getUuid());
		latchFor(future);
		assertSuccess(future);

		SchemaResponse restSchema = future.result();
		assertThat(restSchema).matches(schema);

		Future<SchemaListResponse> listFuture = getClient().findSchemas(PROJECT_NAME);
		latchFor(listFuture);
		assertSuccess(listFuture);

		//final String removedProjectName = project.getName();
		assertEquals("The removed schema should not be listed in the response", 0,
				listFuture.result().getData().stream().filter(s -> s.getUuid().equals(schema.getUuid())).count());
		project.getSchemaContainerRoot().reload();
		assertFalse("The schema should no longer be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();

		assertTrue("The schema should be assigned to the project.", project.getSchemaContainerRoot().contains(schema));
		// Revoke update perms on the project
		role().revokePermissions(project, UPDATE_PERM);

		Future<SchemaResponse> future = getClient().removeSchemaFromProject(schema.getUuid(), project.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", project.getUuid());

		// Reload the schema and check for expected changes
		assertTrue("The schema should still be listed for the project.", project.getSchemaContainerRoot().contains(schema));
	}

}
