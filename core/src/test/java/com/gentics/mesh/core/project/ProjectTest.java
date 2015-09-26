package com.gentics.mesh.core.project;

import static com.gentics.mesh.util.MeshAssert.assertDeleted;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class ProjectTest extends AbstractBasicObjectTest {

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
	public void testDelete() throws Exception {
		String uuid = project().getUuid();

		Map<String, String> uuidToBeDeleted = new HashMap<>();
		uuidToBeDeleted.put("project", uuid);
		uuidToBeDeleted.put("project.tagFamilyRoot", project().getTagFamilyRoot().getUuid());
		uuidToBeDeleted.put("project.schemaContainerRoot", project().getSchemaContainerRoot().getUuid());
		uuidToBeDeleted.put("project.nodeRoot", project().getNodeRoot().getUuid());

		Project project = project();
		project.delete();

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);

		assertDeleted(uuidToBeDeleted);

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
		Page<? extends Project> page = meshRoot().getProjectRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Project> projects = meshRoot().getProjectRoot().findAll();
		assertNotNull(projects);
		assertEquals(1, projects.size());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(meshRoot().getProjectRoot().findByName("bogus"));
		assertNotNull(meshRoot().getProjectRoot().findByName("dummy"));
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		meshRoot().getProjectRoot().findByUuid(project().getUuid(), rh -> {
			assertNotNull(rh.result());
			latch.countDown();
		});
		meshRoot().getProjectRoot().findByUuid("bogus", rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		Project project = project();
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		project.transformToRest(ac, rh -> {
			assertNotNull(rh.result());
			ProjectResponse response = rh.result();
			assertEquals(project.getName(), response.getName());
			assertEquals(project.getUuid(), response.getUuid());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		Project project = meshRoot().getProjectRoot().create("newProject", user());
		assertNotNull(project);
		String uuid = project.getUuid();
		CountDownLatch latch = new CountDownLatch(2);
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			latch.countDown();
		});
		project.delete();
		// TODO check for attached nodes
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		InternalActionContext ac = getMockedInternalActionContext("");
		Project project = root.getProjectRoot().create("TestProject", user());
		assertFalse(user().hasPermission(ac, project, GraphPermission.CREATE_PERM));
		user().addCRUDPermissionOnRole(root.getProjectRoot(), GraphPermission.CREATE_PERM, project);
		ac.data().clear();
		assertTrue(user().hasPermission(ac, project, GraphPermission.CREATE_PERM));
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
		Project newProject;
		newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(GraphPermission.READ_PERM, newProject);
	}

	@Test
	@Override
	public void testDeletePermission() {
		Project newProject;
		newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(GraphPermission.DELETE_PERM, newProject);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		Project newProject;
		newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(GraphPermission.UPDATE_PERM, newProject);
	}

	@Test
	@Override
	public void testCreatePermission() {
		Project newProject;
		newProject = meshRoot().getProjectRoot().create("newProject", user());
		testPermission(GraphPermission.CREATE_PERM, newProject);
	}

}
