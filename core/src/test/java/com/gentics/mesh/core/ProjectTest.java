package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;

public class ProjectTest extends AbstractBasicObjectTest {

	private ProjectRoot projectRoot;

	@Before
	public void setup() throws Exception {
		super.setup();
		projectRoot = boot.projectRoot();
	}

	@Test
	@Override
	public void testCreate() {
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project project = projectRoot.create("test", user());
		Project project2 = projectRoot.findByName(project.getName());
		assertNotNull(project2);
		assertEquals("test", project2.getName());
		assertEquals(project.getUuid(), project2.getUuid());
	}

	@Test
	@Override
	public void testDelete() {
		String uuid = project().getUuid();
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			Project project = project();
			project.delete();
			tx.success();
		}
		projectRoot.findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
		// TODO assert on tag families of the project
	}

	@Test
	@Override
	public void testRootNode() {
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		int nProjectsBefore = projectRoot.findAll().size();
		assertNotNull(projectRoot.create("test1234556", user()));
		int nProjectsAfter = projectRoot.findAll().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		Page<? extends Project> page = projectRoot.findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Project> projects = projectRoot.findAll();
		assertNotNull(projects);
		assertEquals(1, projects.size());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(projectRoot.findByName("bogus"));
		assertNotNull(projectRoot.findByName("dummy"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		projectRoot.findByUuid(project().getUuid(), rh -> {
			assertNotNull(rh.result());
		});
		projectRoot.findByUuid("bogus", rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		Project project = project();
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		project.transformToRest(rc, rh -> {
			assertNotNull(rh.result());
			ProjectResponse response = rh.result();
			assertEquals(project.getName(), response.getName());
			assertEquals(project.getUuid(), response.getUuid());
			latch.countDown();
		});
		latch.await();
	}

	@Test
	@Override
	public void testCreateDelete() {
		Project project = meshRoot().getProjectRoot().create("newProject", user());
		assertNotNull(project);
		String uuid = project.getUuid();
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
		});
		project.delete();
		// TODO check for attached nodes
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		Project project = root.getProjectRoot().create("TestProject", user());
		assertFalse(user().hasPermission(project, Permission.CREATE_PERM));
		user().addCRUDPermissionOnRole(root.getProjectRoot(), Permission.CREATE_PERM, project);
		assertTrue(user().hasPermission(project, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() {
		Project project = project();
		assertNotNull(project.getName());
		assertEquals("dummy", project.getName());
		assertNotNull(project.getBaseNode());
		assertNotNull(project.getLanguages());
		assertEquals(2, project.getLanguages().size());
		assertEquals(3, project.getSchemaContainerRoot().findAll().size());
	}

	@Test
	@Override
	public void testUpdate() {
		Project project = project();
		project.setName("new Name");
		assertEquals("new Name", project.getName());

		// TODO test root nodes

	}

	@Test
	@Override
	public void testReadPermission() {
		Project newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(Permission.READ_PERM, newProject);
	}

	@Test
	@Override
	public void testDeletePermission() {
		Project newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(Permission.DELETE_PERM, newProject);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		Project newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(Permission.UPDATE_PERM, newProject);
	}

	@Test
	@Override
	public void testCreatePermission() {
		Project newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(Permission.CREATE_PERM, newProject);
	}

}
