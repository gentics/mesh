package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.verticle.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SchemaProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return schemaVerticle;
	}

	// Schema Project Testcases - PUT / Add

	@Test
	public void testAddSchemaToProjectWithPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject", user());

		// Add only read perms
		role().addPermissions(schema, READ_PERM);
		role().addPermissions(extraProject, UPDATE_PERM);

		Future<SchemaResponse> future = getClient().addSchemaToProject(schema.getUuid(), extraProject.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		test.assertSchema(schema, restSchema);

		CountDownLatch latch = new CountDownLatch(1);
		extraProject.getSchemaRoot().findByUuid(schema.getUuid(), rh -> {
			assertNotNull("The schema should be added to the extra project", rh.result());
			latch.countDown();
		});
		latch.await();

	}

	@Test
	public void testAddSchemaToProjectWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project extraProject = projectRoot.create("extraProject", user());
		// Add only read perms
		role().addPermissions(schema, READ_PERM);
		role().addPermissions(project, READ_PERM);

		Future<SchemaResponse> future = getClient().addSchemaToProject(schema.getUuid(), extraProject.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraProject.getUuid());

		// Reload the schema and check for expected changes
		assertFalse("The schema should not have been added to the extra project", extraProject.getSchemaRoot().contains(schema));

	}

	// Schema Project Testcases - DELETE / Remove
	@Test
	public void testRemoveSchemaFromProjectWithPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();
		assertTrue("The schema should be assigned to the project.", project.getSchemaRoot().contains(schema));

		Future<SchemaResponse> future = getClient().removeSchemaFromProject(schema.getUuid(), project.getUuid());
		latchFor(future);
		assertSuccess(future);
		SchemaResponse restSchema = future.result();
		test.assertSchema(schema, restSchema);

		final String removedProjectName = project.getName();
		assertFalse(restSchema.getProjects().stream().filter(p -> p.getName() == removedProjectName).findFirst().isPresent());

		// Reload the schema and check for expected changes
		assertFalse("The schema should not be assigned to the project.", project.getSchemaRoot().contains(schema));
	}

	@Test
	public void testRemoveSchemaFromProjectWithoutPerm() throws Exception {
		SchemaContainer schema = schemaContainer("content");
		Project project = project();

		assertTrue("The schema should be assigned to the project.", project.getSchemaRoot().contains(schema));

		// Revoke update perms on the project
		role().revokePermissions(project, UPDATE_PERM);
		Future<SchemaResponse> future = getClient().removeSchemaFromProject(schema.getUuid(), project.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", project.getUuid());

		// Reload the schema and check for expected changes
		assertTrue("The schema should still be listed for the project.", project.getSchemaRoot().contains(schema));
	}

}
