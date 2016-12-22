package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static com.gentics.mesh.util.MeshAssert.assertAffectedElements;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class ProjectTest extends AbstractBasicIsolatedObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTx noTx = db.noTx()) {
			ProjectReference reference = project().transformToReference();
			assertNotNull(reference);
			assertEquals(project().getUuid(), reference.getUuid());
			assertEquals(project().getName(), reference.getName());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (NoTx noTx = db.noTx()) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			Project project = projectRoot.create("test", user(), schemaContainer("folder").getLatestVersion());
			Project project2 = projectRoot.findByName(project.getName());
			assertNotNull(project2);
			assertEquals("test", project2.getName());
			assertEquals(project.getUuid(), project2.getUuid());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {

		try (NoTx noTx = db.noTx()) {
			String uuid = project().getUuid();

			Map<String, ElementEntry> affectedElements = new HashMap<>();
			// The project
			affectedElements.put("project", new ElementEntry(DELETE_ACTION, uuid));

			// Meta vertices
			affectedElements.put("project.tagFamilyRoot", new ElementEntry(null, project().getTagFamilyRoot().getUuid()));
			affectedElements.put("project.schemaContainerRoot", new ElementEntry(null, project().getSchemaContainerRoot().getUuid()));
			affectedElements.put("project.nodeRoot", new ElementEntry(null, project().getNodeRoot().getUuid()));
			affectedElements.put("project.baseNode", new ElementEntry(null, project().getBaseNode().getUuid()));

			// Nodes
			int i = 0;
			for (Node node : project().getNodeRoot().findAll()) {
				if (!node.getUuid().equals(project().getBaseNode().getUuid())) {
					affectedElements.put("project node " + i, new ElementEntry(DELETE_ACTION, node.getUuid(), node.getAvailableLanguageNames()));
					i++;
				}
			}

			// Project tags
			for (Tag tag : project().getTagRoot().findAll()) {
				affectedElements.put("project tag " + tag.getName(), new ElementEntry(DELETE_ACTION, tag.getUuid()));
			}

			// Project tagFamilies
			for (TagFamily tagFamily : project().getTagFamilyRoot().findAll()) {
				affectedElements.put("project tagfamily " + tagFamily.getName(), new ElementEntry(DELETE_ACTION, tagFamily.getUuid()));
			}

			SearchQueueBatch batch = createBatch();
			Project project = project();
			project.delete(batch);
			batch.reload();
			assertElement(meshRoot().getProjectRoot(), uuid, false);
			assertAffectedElements(affectedElements, batch);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (NoTx noTx = db.noTx()) {
			ProjectRoot projectRoot = meshRoot().getProjectRoot();
			int nProjectsBefore = projectRoot.findAll().size();
			assertNotNull(projectRoot.create("test1234556", user(), schemaContainer("folder").getLatestVersion()));
			int nProjectsAfter = projectRoot.findAll().size();
			assertEquals(nProjectsBefore + 1, nProjectsAfter);
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			Page<? extends Project> page = meshRoot().getProjectRoot().findAll(getMockedInternalActionContext(user()),
					new PagingParametersImpl(1, 25));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (NoTx noTx = db.noTx()) {
			List<? extends Project> projects = meshRoot().getProjectRoot().findAll();
			assertNotNull(projects);
			assertEquals(1, projects.size());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTx noTx = db.noTx()) {
			assertNull(meshRoot().getProjectRoot().findByName("bogus"));
			assertNotNull(meshRoot().getProjectRoot().findByName("dummy"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = meshRoot().getProjectRoot().findByUuid(project().getUuid());
			assertNotNull(project);
			project = meshRoot().getProjectRoot().findByUuid("bogus");
			assertNull(project);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);
			ProjectResponse response = project.transformToRest(ac, 0).toBlocking().value();

			assertEquals(project.getName(), response.getName());
			assertEquals(project.getUuid(), response.getUuid());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = meshRoot().getProjectRoot().create("newProject", user(), schemaContainer("folder").getLatestVersion());
			assertNotNull(project);
			String uuid = project.getUuid();
			SearchQueueBatch batch = createBatch();
			Project foundProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNotNull(foundProject);
			project.delete(batch);
			// TODO check for attached nodes
			foundProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNull(foundProject);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (NoTx noTx = db.noTx()) {
			MeshRoot root = meshRoot();
			InternalActionContext ac = getMockedInternalActionContext();
			Project project = root.getProjectRoot().create("TestProject", user(), schemaContainer("folder").getLatestVersion());
			assertFalse("The user should not have create permissions on the project.",user().hasPermission(project, GraphPermission.CREATE_PERM));
			user().addCRUDPermissionOnRole(root.getProjectRoot(), GraphPermission.CREATE_PERM, project);
			ac.data().clear();
			assertTrue(user().hasPermission(project, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (NoTx noTx = db.noTx()) {
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
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			project.setName("new Name");
			assertEquals("new Name", project.getName());

			// TODO test root nodes
		}

	}

	@Test
	@Override
	public void testReadPermission() {
		try (NoTx noTx = db.noTx()) {
			Project newProject;
			newProject = meshRoot().getProjectRoot().create("newProject", user(), schemaContainer("folder").getLatestVersion());
			testPermission(GraphPermission.READ_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTx noTx = db.noTx()) {
			Project newProject;
			newProject = meshRoot().getProjectRoot().create("newProject", user(), schemaContainer("folder").getLatestVersion());
			testPermission(GraphPermission.DELETE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTx noTx = db.noTx()) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user(), schemaContainer("folder").getLatestVersion());
			testPermission(GraphPermission.UPDATE_PERM, newProject);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTx noTx = db.noTx()) {
			Project newProject = meshRoot().getProjectRoot().create("newProject", user(), schemaContainer("folder").getLatestVersion());
			testPermission(GraphPermission.CREATE_PERM, newProject);
		}
	}

}
