package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.google.common.collect.Iterators;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ProjectTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			ProjectReference reference = project().transformToReference();
			assertNotNull(reference);
			assertEquals(project().getUuid(), reference.getUuid());
			assertEquals(project().getName(), reference.getName());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (Tx tx = tx()) {
			HibProject project = createProject("test", "folder");
			HibProject project2 = tx.projectDao().findByName(project.getName());
			assertNotNull(project2);
			assertEquals("test", project2.getName());
			assertEquals(project.getUuid(), project2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			HibProject project = Tx.get().projectDao().findByUuid(project().getUuid());
			tx.projectDao().delete(project);
			assertElement(tx.projectDao(), projectUuid(), false);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			long nProjectsBefore = tx.projectDao().findAll().count();
			assertNotNull(createProject("test1234556", "folder"));
			long nProjectsAfter = tx.projectDao().findAll().count();
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Page<? extends HibProject> page = tx.projectDao().findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (Tx tx = tx()) {
			long size = Iterators.size(tx.projectDao().findAll().iterator());
			assertEquals(1, size);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			assertNull(tx.projectDao().findByName("bogus"));
			assertNotNull(tx.projectDao().findByName("dummy"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			assertNotNull(project);
			project = tx.projectDao().findByUuid("bogus");
			assertNull(project);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			HibProject project = project();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			ProjectResponse response = tx.projectDao().transformToRestSync(project, ac, 0);

			assertEquals(project.getName(), response.getName());
			assertEquals(project.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		String uuid = tx(tx -> {
			HibProject project = createProject("newProject", "folder");
			assertNotNull(project);
			return project.getUuid();
		});

		tx(tx -> {
			HibProject foundProject = tx.projectDao().findByUuid(uuid);
			assertNotNull(foundProject);
			tx.projectDao().delete(foundProject);
			// TODO check for attached nodes
			foundProject = tx.projectDao().findByUuid(uuid);
			assertNull(foundProject);
		});
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			InternalActionContext ac = mockActionContext();
			// 1. Give the user create on the project root
			roleDao.grantPermissions(role(), tx.data().permissionRoots().project(), CREATE_PERM);
			// 2. Create the project
			HibProject project = createProject("TestProject", "folder");
			assertFalse("The user should not have create permissions on the project.", userDao.hasPermission(user(), project, CREATE_PERM));
			userDao.inheritRolePermissions(user(), tx.data().permissionRoots().project(), project);
			// 3. Assert that the crud permissions (eg. CREATE) was inherited
			ac.data().clear();
			assertTrue("The users role should have inherited the initial permission on the project root.",
				userDao.hasPermission(user(), project, CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			HibProject project = project();
			assertNotNull(project.getName());
			assertEquals("dummy", project.getName());
			assertNotNull(project.getBaseNode());
			assertEquals(3, schemaDao.findAll(project).count());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			HibProject project = project();
			project.setName("new Name");
			assertEquals("new Name", project.getName());

			// TODO test root nodes
		}

	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			HibProject newProject = createProject("newProject", "folder");
			testPermission(InternalPermission.READ_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			HibProject newProject = createProject("newProject", "folder");
			testPermission(InternalPermission.DELETE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			HibProject newProject = createProject("newProject", "folder");
			testPermission(InternalPermission.UPDATE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			HibProject newProject = createProject("newProject", "folder");
			testPermission(InternalPermission.CREATE_PERM, newProject);
		}
	}

	@Test
	public void testDeleteWithTaggedBranches() {
		int numBranches = 5;

		String projectUuid = tx(() -> project().getUuid());
		String initialBranchUuid = tx(() -> project().getInitialBranch().getUuid());

		TagFamilyResponse tagFamily = createTagFamily(TestDataProvider.PROJECT_NAME, "Colours");
		TagResponse red = createTag(TestDataProvider.PROJECT_NAME, tagFamily.getUuid(), "Red");
		TagResponse green = createTag(TestDataProvider.PROJECT_NAME, tagFamily.getUuid(), "Green");
		TagResponse blue = createTag(TestDataProvider.PROJECT_NAME, tagFamily.getUuid(), "Blue");

		List<String> branchUuids = new ArrayList<>();
		for (int i = 0; i < numBranches; i++) {
			BranchCreateRequest request = new BranchCreateRequest()
					.setName(String.format("Branch %d", i))
					.setBaseBranch(new BranchReference().setUuid(initialBranchUuid))
					.setLatest(true);
			AtomicReference<String> branchUuid = new AtomicReference<>();
			waitForJobs(() -> {
				branchUuid.set(call(() -> client().createBranch(TestDataProvider.PROJECT_NAME, request)).getUuid());
			}, JobStatus.COMPLETED, 1);
			branchUuids.add(branchUuid.get());

			BranchCreateRequest subrequest = new BranchCreateRequest()
					.setName(String.format("Subbranch %d", i))
					.setBaseBranch(new BranchReference().setUuid(branchUuid.get()))
					.setLatest(false);
			waitForJobs(() -> {
				branchUuids.add(call(() -> client().createBranch(TestDataProvider.PROJECT_NAME, subrequest)).getUuid());
			}, JobStatus.COMPLETED, 1);
		}
		for (String branchUuid : branchUuids) {
			for (TagResponse tag : Arrays.asList(red, green, blue)) {
				call(() -> client().addTagToBranch(TestDataProvider.PROJECT_NAME, branchUuid, tag.getUuid()));
			}
		}

		tx(tx -> {
			HibProject project = tx.projectDao().findByUuid(project().getUuid());
			tx.projectDao().delete(project);
		});

		tx(tx -> {
			assertThat(tx.projectDao().findByUuid(projectUuid)).as("Deleted project").isNull();
		});
	}
}
