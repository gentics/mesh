package com.gentics.mesh.core;

import static org.junit.Assert.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class TagTest extends AbstractBasicObjectTest {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	@Autowired
	private TagService tagService;

	@Autowired
	private NodeService nodeService;

	@Test
	public void testTagFamilyTagCreation() {
		final String TAG_FAMILY_NAME = "mycustomtagFamily";
		TagFamily tagFamily = data().getProject().getTagFamilyRoot().create(TAG_FAMILY_NAME);
		assertNotNull(tagFamily);
		assertEquals(TAG_FAMILY_NAME, tagFamily.getName());
		assertNull(tagFamily.getDescription());
		tagFamily.setDescription("description");
		assertEquals("description", tagFamily.getDescription());
		assertEquals(0, tagFamily.getTags().size());
		Tag tag = tagFamily.create(GERMAN_NAME);
		assertEquals(1, tagFamily.getTags().size());
	}

	@Test
	public void testSimpleTag() {
		TagFamily root = data().getTagFamily("basic");
		Tag tag = root.create("test");
		assertEquals("test", tag.getName());
		tag.setName("test2");
		assertEquals("test2", tag.getName());
	}

	@Test
	public void testNodeTaggging() {

		// 1. Create the tag
		TagFamily root = data().getTagFamily("basic");
		Tag tag = root.create(ENGLISH_NAME);
		String uuid = tag.getUuid();
		tag = tagService.findByUUID(uuid);
		assertNotNull(tag);

		// 2. Create the node
		final String GERMAN_TEST_FILENAME = "german.html";
		Node parentNode = data().getFolder("2015");
		Node node = parentNode.create();
		Language german = languageService.findByLanguageTag("de");
		NodeFieldContainer germanContainer = node.getOrCreateFieldContainer(german);

		germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
		germanContainer.createString("name").setString("german node name");

		// 3. Assign the tag to the node
		node.addTag(tag);

		// 4. Reload the tag and inspect the tagged nodes
		tag = tagService.findByUUID(tag.getUuid());

		assertEquals("The tag should have exactly one node.", 1, tag.getNodes().size());
		Node contentFromTag = tag.getNodes().iterator().next();
		NodeFieldContainer fieldContainer = contentFromTag.getFieldContainer(german);

		assertNotNull(contentFromTag);
		assertEquals("We did not get the correct content.", node.getUuid(), contentFromTag.getUuid());
		String filename = fieldContainer.getString("displayName").getString();
		assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

		// Remove the file/content and check whether the content was really removed
		tag.removeNode(contentFromTag);
		// TODO verify for removed node
		assertEquals("The tag should not have any file.", 0, tag.getNodes().size());

	}

	@Test
	public void testNodeTagging() {
		final String TEST_TAG_NAME = "testTag";
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create(TEST_TAG_NAME);

		Node node = data().getFolder("news");
		node.addTag(tag);

		Node reloadedNode = nodeService.findByUUID(node.getUuid());
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
		List<String> languageTags = new ArrayList<>();
		languageTags.add("de");
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);

		Page<? extends Tag> tagPage = tagService.findProjectTags(requestUser, "dummy", languageTags, new PagingInfo(1, 10));
		assertEquals(12, tagPage.getTotalElements());
		assertEquals(10, tagPage.getSize());

		languageTags.add("en");
		tagPage = tagService.findProjectTags(requestUser, "dummy", languageTags, new PagingInfo(1, 14));
		assertEquals(data().getTags().size(), tagPage.getTotalElements());
		assertEquals(12, tagPage.getSize());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByUUID() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreate() {
		TagFamily tagFamily = data().getTagFamily("basic");
		Tag tag = tagFamily.create(GERMAN_NAME);
		assertNotNull(tag);
		String uuid = tag.getUuid();
		tag = tagService.findByUUID(uuid);
		assertNotNull("The folder could not be found.", tag);
		String name = tag.getName();
		assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
		assertEquals(10, tagFamily.getTags().size());
	}

	@Test
	@Override
	public void testTransformation() {
		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		List<String> languageTags = new ArrayList<>();
		languageTags.add("en");
		languageTags.add("de");
		int depth = 3;

		RoutingContext rc = getMockedRoutingContext("lang=de,en");
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
		for (int i = 0; i < 100; i++) {
			long start = System.currentTimeMillis();
			TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
			TagResponse response = tag.transformToRest(info);
			assertNotNull(response);
			long dur = System.currentTimeMillis() - start;
			log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
			System.out.println(JsonUtil.toJson(response));
		}
		// assertEquals(2, response.getChildTags().size());
		// assertEquals(4, response.getPerms().length);

	}

	@Test
	@Override
	public void testCreateDelete() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRead() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDelete() {
		Tag tag = data().getTag("red");
		String uuid = tag.getUuid();
		tag.remove();
		assertNull(tagService.findByUUID(uuid));

	}

	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDeletePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdatePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreatePermission() {
		fail("Not yet implemented");
	}
}
