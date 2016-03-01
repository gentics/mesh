package com.gentics.mesh.core.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagTest extends AbstractBasicObjectTest {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		Tag tag = tag("red");
		InternalActionContext ac = getMockedInternalActionContext("");
		TagReference reference = tag.transformToReference(ac);
		assertNotNull(reference);
		assertEquals(tag.getUuid(), reference.getUuid());
		assertEquals(tag.getName(), reference.getName());
	}

	@Test
	public void testTagFamilyTagCreation() {
		final String TAG_FAMILY_NAME = "mycustomtagFamily";
		TagFamily tagFamily = project().getTagFamilyRoot().create(TAG_FAMILY_NAME, user());
		assertNotNull(tagFamily);
		assertEquals(TAG_FAMILY_NAME, tagFamily.getName());
		assertNull(tagFamily.getDescription());
		tagFamily.setDescription("description");
		assertEquals("description", tagFamily.getDescription());
		assertEquals(0, tagFamily.getTagRoot().findAll().size());
		assertNotNull(tagFamily.create(GERMAN_NAME, project(), user()));
		assertEquals(1, tagFamily.getTagRoot().findAll().size());
	}

	@Test
	public void testReadFieldContainer() {
		Tag tag = tags().get("red");
		assertEquals(1, tag.getFieldContainers().size());
	}

	@Test
	public void testSimpleTag() {
		TagFamily root = tagFamily("basic");
		Tag tag = root.create("test", project(), user());
		assertEquals("test", tag.getName());
		tag.setName("test2");
		assertEquals("test2", tag.getName());
	}

	@Test
	public void testProjectTag() {
		TagFamily root = tagFamily("basic");
		Tag tag = root.create("test", project(), user());
		assertEquals(project(), tag.getProject());
	}

	@Test
	public void testNodeTaggging() throws Exception {
		// 1. Create the tag
		TagFamily root = tagFamily("basic");
		Tag tag = root.create(ENGLISH_NAME, project(), user());
		String uuid = tag.getUuid();
		assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().first());

		// 2. Create the node
		final String GERMAN_TEST_FILENAME = "german.html";
		Node parentNode = folder("2015");
		Node node = parentNode.create(user(), getSchemaContainer(), project());
		Language german = boot.languageRoot().findByLanguageTag("de");
		NodeGraphFieldContainer germanContainer = node.getOrCreateGraphFieldContainer(german);

		germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
		germanContainer.createString("name").setString("german node name");

		// 3. Assign the tag to the node
		node.addTag(tag);

		// 4. Reload the tag and inspect the tagged nodes
		Tag reloadedTag = meshRoot().getTagRoot().findByUuid(tag.getUuid()).toBlocking().first();
		assertEquals("The tag should have exactly one node.", 1, reloadedTag.getNodes().size());
		Node contentFromTag = reloadedTag.getNodes().iterator().next();
		NodeGraphFieldContainer fieldContainer = contentFromTag.getGraphFieldContainer(german);

		assertNotNull(contentFromTag);
		assertEquals("We did not get the correct content.", node.getUuid(), contentFromTag.getUuid());
		String filename = fieldContainer.getString("displayName").getString();
		assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

		// Remove the file/content and check whether the content was really removed
		reloadedTag.removeNode(contentFromTag);
		// TODO verify for removed node
		assertEquals("The tag should not have any file.", 0, reloadedTag.getNodes().size());

	}

	@Test
	public void testNodeTagging() throws Exception {
		final String TEST_TAG_NAME = "testTag";
		TagFamily tagFamily = tagFamily("basic");
		Tag tag = tagFamily.create(TEST_TAG_NAME, project(), user());

		Node node = folder("news");
		node.addTag(tag);

		Node reloadedNode = boot.nodeRoot().findByUuid(node.getUuid()).toBlocking().single();
		boolean found = false;
		for (Tag currentTag : reloadedNode.getTags()) {
			if (currentTag.getUuid().equals(tag.getUuid())) {
				found = true;
			}
		}
		assertTrue("The tag {" + tag.getUuid() + "} was not found within the node tags.", found);

	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();

		PageImpl<? extends Tag> tagPage = meshRoot().getTagRoot().findAll(requestUser, new PagingParameter(1, 10));
		assertEquals(12, tagPage.getTotalElements());
		assertEquals(10, tagPage.getSize());

		tagPage = meshRoot().getTagRoot().findAll(requestUser, new PagingParameter(1, 14));
		assertEquals(tags().size(), tagPage.getTotalElements());
		assertEquals(12, tagPage.getSize());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		TagFamily basicTagFamily = tagFamily("basic");
		Tag noPermTag = basicTagFamily.create("noPermTag", project(), user());
		basicTagFamily.getTagRoot().addTag(noPermTag);
		assertNotNull(noPermTag.getUuid());
		assertEquals(tags().size() + 1, meshRoot().getTagRoot().findAll().size());

		PageImpl<? extends Tag> projectTagpage = project().getTagRoot().findAll(getRequestUser(), new PagingParameter(1, 20));
		assertPage(projectTagpage, tags().size());

		PageImpl<? extends Tag> globalTagPage = meshRoot().getTagRoot().findAll(getRequestUser(), new PagingParameter(1, 20));
		assertPage(globalTagPage, tags().size());

		role().grantPermissions(noPermTag, READ_PERM);
		globalTagPage = meshRoot().getTagRoot().findAll(getRequestUser(), new PagingParameter(1, 20));
		assertPage(globalTagPage, tags().size() + 1);
	}

	private void assertPage(PageImpl<? extends Tag> page, int expectedTagCount) {
		assertNotNull(page);

		int nTags = 0;
		for (Tag tag : page) {
			assertNotNull(tag.getName());
			nTags++;
		}
		assertEquals("The page did not contain the correct amount of tags", expectedTagCount, nTags);
		assertEquals(expectedTagCount, page.getTotalElements());
		assertEquals(1, page.getNumber());
		assertEquals(1, page.getTotalPages());

	}

	@Test
	@Override
	public void testRootNode() {
		TagRoot root = meshRoot().getTagRoot();
		assertEquals(tags().size(), root.findAll().size());
		Tag tag = tag("red");
		root.removeTag(tag);
		assertEquals(tags().size() - 1, root.findAll().size());
		root.removeTag(tag);
		assertEquals(tags().size() - 1, root.findAll().size());
		root.reload();
		tag.reload();
		root.addTag(tag);
		assertEquals(tags().size(), root.findAll().size());
		root.addTag(tag);
		assertEquals(tags().size(), root.findAll().size());
		root.delete();
	}

	@Test
	@Override
	public void testFindByName() {
		Tag tag = tag("car");
		Tag foundTag = meshRoot().getTagRoot().findByName("Car").toBlocking().single();
		assertNotNull(foundTag);
		assertEquals("Car", foundTag.getName());
		assertNotNull(meshRoot().getTagRoot().findByName(tag.getName()).toBlocking().single());
		assertNull("No tag with the name bogus should be found", meshRoot().getTagRoot().findByName("bogus").toBlocking().single());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		Tag tag = tag("car");
		assertNotNull("The tag with the uuid could not be found", meshRoot().getTagRoot().findByUuid(tag.getUuid()).toBlocking().single());
		assertNull("A tag with the a bogus uuid should not be found but it was.", meshRoot().getTagRoot().findByUuid("bogus").toBlocking().single());
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		TagFamily tagFamily = tagFamily("basic");
		Tag tag = tagFamily.create(GERMAN_NAME, project(), user());
		assertNotNull(tag);
		String uuid = tag.getUuid();
		CountDownLatch latch = new CountDownLatch(1);
		Tag loadedTag = meshRoot().getTagRoot().findByUuid(uuid).toBlocking().single();
		assertNotNull("The folder could not be found.", loadedTag);
		String name = loadedTag.getName();
		assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
		assertEquals(10, tagFamily.getTagRoot().findAll().size());
		latch.countDown();
		Tag projectTag = tagFamily.getTagRoot().findByUuid(uuid).toBlocking().single();
		assertNotNull("The tag should also be assigned to the project tag root", projectTag);

	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		Tag tag = tag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		languageTags.add("de");
		int depth = 3;

		RoutingContext rc = getMockedRoutingContext("lang=de,en");
		InternalActionContext ac = InternalActionContext.create(rc);
		int nTransformations = 100;
		for (int i = 0; i < nTransformations; i++) {
			long start = System.currentTimeMillis();
			TagResponse response = tag.transformToRest(ac).toBlocking().first();

			assertNotNull(response);
			long dur = System.currentTimeMillis() - start;
			log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
			JsonUtil.toJson(response);
		}
		// assertEquals(2, response.getChildTags().size());
		// assertEquals(4, response.getPerms().length);

	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		TagFamily tagFamily = tagFamily("basic");
		Tag tag = tagFamily.create("someTag", project(), user());
		String uuid = tag.getUuid();
		assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().single());
		tag.delete();
		assertNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().single());
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		TagFamily tagFamily = tagFamily("basic");
		InternalActionContext ac = getMockedInternalActionContext("");
		Tag tag = tagFamily.create("someTag", project(), user());
		assertTrue(user().hasPermissionAsync(ac, tagFamily, GraphPermission.READ_PERM).toBlocking().first());
		assertFalse(user().hasPermissionAsync(ac, tag, GraphPermission.READ_PERM).toBlocking().first());
		getRequestUser().addCRUDPermissionOnRole(tagFamily, GraphPermission.CREATE_PERM, tag);
		ac.data().clear();
		assertTrue(user().hasPermissionAsync(ac, tag, GraphPermission.READ_PERM).toBlocking().first());
	}

	@Test
	@Override
	public void testRead() {
		Tag tag = tag("car");
		assertEquals("Car", tag.getName());
		assertNotNull(tag.getCreationTimestamp());
		assertNotNull(tag.getLastEditedTimestamp());
		assertNotNull(tag.getEditor());
		assertNotNull(tag.getCreator());
		assertNotNull(tag.getTagFamily());
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		Tag tag = tag("red");
		String uuid = tag.getUuid();
		tag.remove();
		assertElement(meshRoot().getTagRoot(), uuid, false);
	}

	@Test
	@Override
	public void testUpdate() {
		Tag tag = tag("red");
		tag.setName("Blue");
		assertEquals("Blue", tag.getName());
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(GraphPermission.READ_PERM, tag("red"));
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(GraphPermission.DELETE_PERM, tag("red"));
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(GraphPermission.UPDATE_PERM, tag("red"));
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(GraphPermission.CREATE_PERM, tag("red"));
	}

}
