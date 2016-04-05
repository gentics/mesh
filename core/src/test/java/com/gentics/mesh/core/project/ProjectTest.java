package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
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
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class ProjectTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		ProjectReference reference = project().transformToReference();
		assertNotNull(reference);
		assertEquals(project().getUuid(), reference.getUuid());
		assertEquals(project().getName(), reference.getName());
	}

	@Test
	@Override
	public void testCreate() {
		ProjectRoot projectRoot = meshRoot().getProjectRoot();
		Project project = projectRoot.create("test", user());
		Project project2 = projectRoot.findByName(project.getName()).toBlocking().single();
		assertNotNull(project2);
		assertEquals("test", project2.getName());
		assertEquals(project.getUuid(), project2.getUuid());
	}

	@Test
	@Override
	public void testDelete() throws Exception {
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
		PageImpl<? extends Project> page = meshRoot().getProjectRoot().findAll(getRequestUser(), new PagingParameter(1, 25));
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
		assertNull(meshRoot().getProjectRoot().findByName("bogus").toBlocking().single());
		assertNotNull(meshRoot().getProjectRoot().findByName("dummy").toBlocking().single());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		Project project = meshRoot().getProjectRoot().findByUuid(project().getUuid()).toBlocking().single();
		assertNotNull(project);
		project = meshRoot().getProjectRoot().findByUuid("bogus").toBlocking().single();
		assertNull(project);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		Project project = project();
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		ProjectResponse response = project.transformToRest(ac, 0).toBlocking().first();

		assertEquals(project.getName(), response.getName());
		assertEquals(project.getUuid(), response.getUuid());
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		Project project = meshRoot().getProjectRoot().create("newProject", user());
		assertNotNull(project);
		String uuid = project.getUuid();
		SearchQueueBatch batch = createBatch();
		Project foundProject = meshRoot().getProjectRoot().findByUuid(uuid).toBlocking().single();
		assertNotNull(foundProject);
		project.delete(batch);
		// TODO check for attached nodes
		foundProject = meshRoot().getProjectRoot().findByUuid(uuid).toBlocking().single();
		assertNull(foundProject);
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		InternalActionContext ac = getMockedInternalActionContext("");
		Project project = root.getProjectRoot().create("TestProject", user());
		assertFalse(user().hasPermissionAsync(ac, project, GraphPermission.CREATE_PERM).toBlocking().single());
		user().addCRUDPermissionOnRole(root.getProjectRoot(), GraphPermission.CREATE_PERM, project);
		ac.data().clear();
		assertTrue(user().hasPermissionAsync(ac, project, GraphPermission.CREATE_PERM).toBlocking().single());
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
