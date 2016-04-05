package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.util.MeshAssert.assertDeleted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class TagFamilyTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		TagFamily tagFamily = tagFamily("colors");
		InternalActionContext ac = getMockedInternalActionContext("");
		TagFamilyReference reference = tagFamily.transformToReference();
		assertNotNull(reference);
		assertEquals(tagFamily.getUuid(), reference.getUuid());
		assertEquals(tagFamily.getName(), reference.getName());
	}

	@Test
	public void testTagFamilyProject() {
		TagFamily tagFamily = tagFamily("colors");
		assertNotNull(tagFamily.getProject());
		assertEquals(project(), tagFamily.getProject());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		TagFamilyRoot root = meshRoot().getTagFamilyRoot();
		root.findAll(getRequestUser(), new PagingParameter(1, 10));
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		TagFamilyRoot root = meshRoot().getTagFamilyRoot();
		List<? extends TagFamily> families = root.findAll();
		assertNotNull(families);
		assertEquals(2, families.size());

		TagFamilyRoot projectTagFamilyRoot = project().getTagFamilyRoot();
		assertNotNull(projectTagFamilyRoot);

		TagFamily projectTagFamily = projectTagFamilyRoot.findByName("colors").toBlocking().single();
		assertNotNull(projectTagFamily);

		assertNotNull(projectTagFamilyRoot.create("bogus", user()));
		assertEquals(3, projectTagFamilyRoot.findAll().size());
		assertEquals(3, root.findAll().size());
	}

	@Test
	@Override
	public void testRootNode() {
		TagFamilyRoot root = project().getTagFamilyRoot();
		int nProjectsBefore = root.findAll().size();
		assertNotNull(root.create("test1234556", user()));
		int nProjectsAfter = root.findAll().size();
		assertEquals(nProjectsBefore + 1, nProjectsAfter);
	}

	@Test
	@Override
	public void testFindByName() {
		TagFamilyRoot root = meshRoot().getTagFamilyRoot();
		assertNotNull(root);
		assertNotNull(root.findByName("colors").toBlocking().single());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		TagFamilyRoot root = project().getTagFamilyRoot();
		TagFamily tagFamily = tagFamily("colors");

		TagFamily foundTagFamily = root.findByUuid(tagFamily.getUuid()).toBlocking().single();
		assertNotNull(foundTagFamily);
	}

	@Test
	@Override
	public void testRead() throws IOException {
		TagFamily tagFamily = tagFamily("colors");
		assertNotNull(tagFamily.getName());
		assertEquals("colors", tagFamily.getName());
		assertNotNull(tagFamily.getEditor());
		assertNotNull(tagFamily.getCreator());
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		TagFamilyRoot root = project().getTagFamilyRoot();
		TagFamily family = root.create("test", user());
		TagFamily family2 = root.findByName(family.getName()).toBlocking().single();
		assertNotNull(family2);
		assertEquals("test", family2.getName());
		assertEquals(family.getUuid(), family2.getUuid());
	}

	@Test
	@Override
	public void testDelete() {
		try (Trx tx = db.trx()) {
			Map<String, ElementEntry> uuidToBeDeleted = new HashMap<>();
			TagFamily tagFamily = tagFamily("colors");
			uuidToBeDeleted.put("tagFamily", new ElementEntry(tagFamily.getUuid()));
			uuidToBeDeleted.put("tagFamily.red", new ElementEntry(tag("red").getUuid()));
			SearchQueueBatch batch = createBatch();
			tagFamily.delete(batch);
			assertDeleted(uuidToBeDeleted);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		TagFamily tagFamily = tagFamily("colors");
		tagFamily.setName("new Name");
		assertEquals("new Name", tagFamily.getName());
	}

	@Test
	@Override
	public void testReadPermission() {
		TagFamily tagFamily = project().getTagFamilyRoot().create("newProject", user());
		testPermission(GraphPermission.READ_PERM, tagFamily);
	}

	@Test
	@Override
	public void testDeletePermission() {
		TagFamily tagFamily;
		tagFamily = project().getTagFamilyRoot().create("newProject", user());
		testPermission(GraphPermission.DELETE_PERM, tagFamily);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		TagFamily tagFamily;
		tagFamily = project().getTagFamilyRoot().create("newProject", user());
		testPermission(GraphPermission.UPDATE_PERM, tagFamily);
	}

	@Test
	@Override
	public void testCreatePermission() {
		TagFamily tagFamily;
		tagFamily = project().getTagFamilyRoot().create("newProject", user());
		testPermission(GraphPermission.CREATE_PERM, tagFamily);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		TagFamily tagFamily = tagFamily("colors");
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		TagFamilyResponse response = tagFamily.transformToRest(ac, 0).toBlocking().single();
		assertNotNull(response);
		assertEquals(tagFamily.getName(), response.getName());
		assertEquals(tagFamily.getUuid(), response.getUuid());
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		TagFamilyRoot root = project().getTagFamilyRoot();
		TagFamily tagFamily = root.create("test123", user());
		assertNotNull(tagFamily);
		String uuid = tagFamily.getUuid();
		TagFamily foundTagFamily = root.findByUuid(uuid).toBlocking().single();
		assertNotNull(foundTagFamily);
		SearchQueueBatch batch = createBatch();
		tagFamily.delete(batch);
		// TODO check for attached nodes
		Project project = meshRoot().getProjectRoot().findByUuid(uuid).toBlocking().single();
		assertNull(project);

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		TagFamilyRoot root = project().getTagFamilyRoot();
		InternalActionContext ac = getMockedInternalActionContext("");
		TagFamily tagFamily = root.create("test123", user());
		assertFalse(user().hasPermissionAsync(ac, tagFamily, GraphPermission.CREATE_PERM).toBlocking().first());
		user().addCRUDPermissionOnRole(root, GraphPermission.CREATE_PERM, tagFamily);
		ac.data().clear();
		assertTrue(user().hasPermissionAsync(ac, tagFamily, GraphPermission.CREATE_PERM).toBlocking().first());
	}

}
