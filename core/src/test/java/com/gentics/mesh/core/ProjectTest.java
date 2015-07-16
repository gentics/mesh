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
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		Project project = projectRoot.create("test", getUser());
		Project project2 = projectRoot.findByName(project.getName());
		assertNotNull(project2);
		assertEquals("test", project2.getName());
		assertEquals(project.getUuid(), project2.getUuid());
	}

	@Test
	@Override
	public void testDelete() {
		String uuid = data().getProject().getUuid();
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			Project project = data().getProject();
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
		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		int nProjectsBefore = projectRoot.findAll().size();
		assertNotNull(projectRoot.create("test1234556", getUser()));
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
		projectRoot.findByUuid(data().getProject().getUuid(), rh -> {
			assertNotNull(rh.result());
		});
		projectRoot.findByUuid("bogus", rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		Project project = data().getProject();
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
		Project project = getMeshRoot().getProjectRoot().create("newProject", getUser());
		assertNotNull(project);
		String uuid = project.getUuid();
		getMeshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
		});
		project.delete();
		// TODO check for attached nodes
		getMeshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = getMeshRoot();
		Project project = root.getProjectRoot().create("TestProject", getUser());
		assertFalse(getUser().hasPermission(project, Permission.CREATE_PERM));
		getUser().addCRUDPermissionOnRole(root.getProjectRoot(), Permission.CREATE_PERM, project);
		assertTrue(getUser().hasPermission(project, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() {
		Project project = getProject();
		assertNotNull(project.getName());
		assertEquals("dummy", project.getName());
		assertNotNull(project.getBaseNode());
		assertNotNull(project.getLanguages());
		assertEquals(2, project.getLanguages().size());
		assertEquals(3, project.getSchemaRoot().findAll().size());
	}

	@Test
	@Override
	public void testUpdate() {
		Project project = getProject();
		project.setName("new Name");
		assertEquals("new Name", project.getName());

		// TODO test root nodes

	}

	@Test
	@Override
	public void testReadPermission() {
		Project newProject = getMeshRoot().getProjectRoot().create("newProject", getUser());
		testPermission(Permission.READ_PERM, newProject);
	}

	@Test
	@Override
	public void testDeletePermission() {
		Project newProject = getMeshRoot().getProjectRoot().create("newProject", getUser());
		testPermission(Permission.DELETE_PERM, newProject);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		Project newProject = getMeshRoot().getProjectRoot().create("newProject", getUser());
		testPermission(Permission.UPDATE_PERM, newProject);
	}

	@Test
	@Override
	public void testCreatePermission() {
		Project newProject = getMeshRoot().getProjectRoot().create("newProject", getUser());
		testPermission(Permission.CREATE_PERM, newProject);
	}

}
