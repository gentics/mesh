package com.gentics.mesh.core;

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
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class ProjectTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testCreate() {
		try (Trx tx = new Trx(db)) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			Project project = projectRoot.create("test", user());
			Project project2 = projectRoot.findByName(project.getName());
			assertNotNull(project2);
			assertEquals("test", project2.getName());
			assertEquals(project.getUuid(), project2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
			String uuid = project().getUuid();

			Map<String, String> uuidToBeDeleted = new HashMap<>();
			uuidToBeDeleted.put("project", uuid);
			uuidToBeDeleted.put("project.tagFamilyRoot", project().getTagFamilyRoot().getUuid());
			uuidToBeDeleted.put("project.schemaContainerRoot", project().getSchemaContainerRoot().getUuid());
			uuidToBeDeleted.put("project.nodeRoot", project().getNodeRoot().getUuid());

			try (Trx txDelete = new Trx(db)) {
				Project project = project();
				project.delete();
				txDelete.success();
			}

			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
				assertNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);

			assertDeleted(uuidToBeDeleted);

			// TODO assert on tag families of the project
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Trx tx = new Trx(db)) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			int nProjectsBefore = projectRoot.findAll().size();
			assertNotNull(projectRoot.create("test1234556", user()));
			int nProjectsAfter = projectRoot.findAll().size();
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Trx tx = new Trx(db)) {
			Page<? extends Project> page = meshRoot().getProjectRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (Trx tx = new Trx(db)) {
			List<? extends Project> projects = meshRoot().getProjectRoot().findAll();
			assertNotNull(projects);
			assertEquals(1, projects.size());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Trx tx = new Trx(db)) {
			assertNull(meshRoot().getProjectRoot().findByName("bogus"));
			assertNotNull(meshRoot().getProjectRoot().findByName("dummy"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
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
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
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
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws InterruptedException {
		try (Trx tx = new Trx(db)) {
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
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Trx tx = new Trx(db)) {
			MeshRoot root = meshRoot();
			Project project = root.getProjectRoot().create("TestProject", user());
			assertFalse(user().hasPermission(project, GraphPermission.CREATE_PERM));
			user().addCRUDPermissionOnRole(root.getProjectRoot(), GraphPermission.CREATE_PERM, project);
			assertTrue(user().hasPermission(project, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Trx tx = new Trx(db)) {
			Project project = project();
			assertNotNull(project.getName());
			assertEquals("dummy", project.getName());
			assertNotNull(project.getBaseNode());
			assertNotNull(project.getLanguages());
			assertEquals(2, project.getLanguages().size());
			assertEquals(3, project.getSchemaContainerRoot().findAll().size());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Trx tx = new Trx(db)) {
			Project project = project();
			project.setName("new Name");
			assertEquals("new Name", project.getName());

			// TODO test root nodes
		}

	}

	@Test
	@Override
	public void testReadPermission() {
		try (Trx tx = new Trx(db)) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user());
			testPermission(GraphPermission.READ_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Trx tx = new Trx(db)) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user());
			testPermission(GraphPermission.DELETE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Trx tx = new Trx(db)) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user());
			testPermission(GraphPermission.UPDATE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Trx tx = new Trx(db)) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user());
			testPermission(GraphPermission.CREATE_PERM, newProject);
		}
	}

}
