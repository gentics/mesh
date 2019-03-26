package com.gentics.mesh.core.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DROP_INDEX;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.google.common.collect.Iterators;
import com.syncleus.ferma.tx.Tx;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = PROJECT, startServer = false)
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
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			Project project = createProject("test", "folder");
			Project project2 = projectRoot.findByName(project.getName());
			assertNotNull(project2);
			assertEquals("test", project2.getName());
			assertEquals(project.getUuid(), project2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {

		try (Tx tx = tx()) {
			String uuid = project().getUuid();

			Map<String, ElementEntry> batchEnttries = new HashMap<>();

			// The project
			batchEnttries.put("project", new ElementEntry(DELETE_ACTION, uuid));

			// Meta vertices
			batchEnttries.put("project.tagFamilyRoot", new ElementEntry(null, project().getTagFamilyRoot().getUuid()));
			batchEnttries.put("project.schemaContainerRoot", new ElementEntry(null, project().getSchemaContainerRoot().getUuid()));
			batchEnttries.put("project.nodeRoot", new ElementEntry(null, project().getNodeRoot().getUuid()));
			batchEnttries.put("project.baseNode", new ElementEntry(null, project().getBaseNode().getUuid()));

			// Nodes
			int i = 0;
			batchEnttries.put("project node index " + i, new ElementEntry(DROP_INDEX, project().getUuid()));
			i++;

			// Project tagFamilies
			for (TagFamily tagFamily : project().getTagFamilyRoot().findAll()) {
				batchEnttries.put("project tagfamily " + tagFamily.getName(), new ElementEntry(DROP_INDEX, tagFamily.getUuid()));

				// tags
				for (Tag tag : tagFamily.findAll()) {
					batchEnttries.put("project tag " + tag.getName(), new ElementEntry(DROP_INDEX, tag.getUuid()));
				}
			}

			BulkActionContext bac = createBulkContext();
			Project project = project();
			project.delete(bac);
			assertElement(meshRoot().getProjectRoot(), uuid, false);
			assertThat(bac.batch()).containsEntries(batchEnttries);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			long nProjectsBefore = projectRoot.findAll().count();
			assertNotNull(createProject("test1234556", "folder"));
			long nProjectsAfter = projectRoot.findAll().count();
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Page<? extends Project> page = meshRoot().getProjectRoot().findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (Tx tx = tx()) {
			long size = Iterators.size(meshRoot().getProjectRoot().findAll().iterator());
			assertEquals(1, size);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			assertNull(meshRoot().getProjectRoot().findByName("bogus"));
			assertNotNull(meshRoot().getProjectRoot().findByName("dummy"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			Project project = meshRoot().getProjectRoot().findByUuid(projectUuid());
			assertNotNull(project);
			project = meshRoot().getProjectRoot().findByUuid("bogus");
			assertNull(project);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			ProjectResponse response = project.transformToRest(ac, 0).blockingGet();

			assertEquals(project.getName(), response.getName());
			assertEquals(project.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			Project project = createProject("newProject", "folder");
			assertNotNull(project);
			String uuid = project.getUuid();
			BulkActionContext bac = createBulkContext();
			Project foundProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNotNull(foundProject);
			project.delete(bac);
			// TODO check for attached nodes
			foundProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNull(foundProject);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			MeshRoot root = meshRoot();
			InternalActionContext ac = mockActionContext();
			// 1. Give the user create on the project root
			role().grantPermissions(meshRoot().getProjectRoot(), CREATE_PERM);
			// 2. Create the project
			Project project = createProject("TestProject", "folder");
			assertFalse("The user should not have create permissions on the project.", user().hasPermission(project, CREATE_PERM));
			user().addCRUDPermissionOnRole(root.getProjectRoot(), CREATE_PERM, project);
			// 3. Assert that the crud permissions (eg. CREATE) was inherited
			ac.data().clear();
			assertTrue("The users role should have inherited the initial permission on the project root.",
				user().hasPermission(project, CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			Project project = project();
			assertNotNull(project.getName());
			assertEquals("dummy", project.getName());
			assertNotNull(project.getBaseNode());
			assertEquals(3, project.getSchemaContainerRoot().findAll().count());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			Project project = project();
			project.setName("new Name");
			assertEquals("new Name", project.getName());

			// TODO test root nodes
		}

	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			Project newProject = createProject("newProject", "folder");
			testPermission(GraphPermission.READ_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			Project newProject = createProject("newProject", "folder");
			testPermission(GraphPermission.DELETE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			Project newProject = createProject("newProject", "folder");
			testPermission(GraphPermission.UPDATE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			Project newProject = createProject("newProject", "folder");
			testPermission(GraphPermission.CREATE_PERM, newProject);
		}
	}

}
