package com.gentics.mesh.core.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static com.gentics.mesh.util.MeshAssert.assertAffectedElements;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.mock.Mocks;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagTest extends AbstractBasicIsolatedObjectTest {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	private NodeMigrationHandler nodeMigrationHandler;

	@Before
	public void setupHandler() {
		this.nodeMigrationHandler = meshDagger.nodeMigrationHandler();
	}

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			TagReference reference = tag.transformToReference();
			assertNotNull(reference);
			assertEquals(tag.getUuid(), reference.getUuid());
			assertEquals(tag.getName(), reference.getName());
		}
	}

	@Test
	public void testTagFamilyTagCreation() {
		try (NoTx noTx = db.noTx()) {
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
	}

	@Test
	public void testReadFieldContainer() {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tags().get("red");
			assertEquals(1, tag.getFieldContainers().size());
		}
	}

	@Test
	public void testSimpleTag() {
		try (NoTx noTx = db.noTx()) {
			TagFamily root = tagFamily("basic");
			Tag tag = root.create("test", project(), user());
			assertEquals("test", tag.getName());
			tag.setName("test2");
			assertEquals("test2", tag.getName());
		}
	}

	@Test
	public void testProjectTag() {
		try (NoTx noTx = db.noTx()) {
			TagFamily root = tagFamily("basic");
			Tag tag = root.create("test", project(), user());
			assertEquals(project(), tag.getProject());
		}
	}

	@Test
	public void testNodeTagging() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release release = project.getLatestRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());

			// 2. Create the node
			final String GERMAN_TEST_FILENAME = "german.html";
			Node parentNode = folder("2015");
			Node node = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project);
			Language german = boot.languageRoot().findByLanguageTag("de");
			NodeGraphFieldContainer germanContainer = node.createGraphFieldContainer(german, release, user());

			germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
			germanContainer.createString("name").setString("german node name");

			// 3. Assign the tag to the node
			node.addTag(tag, release);

			// 4. Reload the tag and inspect the tagged nodes
			Tag reloadedTag = meshRoot().getTagRoot().findByUuid(tag.getUuid()).toBlocking().value();
			assertEquals("The tag should have exactly one node.", 1, reloadedTag.getNodes(release).size());
			Node contentFromTag = reloadedTag.getNodes(release).iterator().next();
			NodeGraphFieldContainer fieldContainer = contentFromTag.getLatestDraftFieldContainer(german);

			assertNotNull(contentFromTag);
			assertEquals("We did not get the correct content.", node.getUuid(), contentFromTag.getUuid());
			String filename = fieldContainer.getString("displayName").getString();
			assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

			// Remove the file/content and check whether the content was really removed
			reloadedTag.removeNode(contentFromTag);
			// TODO verify for removed node
			assertEquals("The tag should not have any file.", 0, reloadedTag.getNodes(release).size());
		}

	}

	@Test
	public void testNodeTaggingInRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());

			// 2. Create new Release
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 3. Migrate nodes to new release
			nodeMigrationHandler.migrateNodes(newRelease);

			// 4. Create and Tag a node
			Node node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project);
			node.addTag(tag, initialRelease);

			// 5. Assert
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release")
					.usingElementComparatorOnFields("uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").isEmpty();
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").isEmpty();

			// 6. Tag in new Release
			node.addTag(tag, newRelease);

			// 7. Assert again
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release")
					.usingElementComparatorOnFields("uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
					.containsOnly(node);
		}
	}

	@Test
	public void testMigrateTagsForRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());

			// 2. Create and Tag a node
			Node node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project);
			node.addTag(tag, initialRelease);
			node.reload();

			// 3. Create new Release
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. Migrate nodes to new release
			nodeMigrationHandler.migrateNodes(newRelease);
			node.reload();
			tag.reload();

			// 5. Assert
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release")
					.usingElementComparatorOnFields("uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
					.containsOnly(node);
		}
	}

	@Test
	public void testNodeUntaggingInRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Release initialRelease = null;
			Release newRelease = null;
			Node node = null;
			Tag tag = null;

			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			initialRelease = project.getInitialRelease();
			tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());

			// 2. Create and Tag a node
			node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project);
			node.addTag(tag, initialRelease);
			node.reload();

			// 3. Create new Release
			newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. Migrate nodes to new release
			nodeMigrationHandler.migrateNodes(newRelease);

			// 5. Untag in initial Release
			node.removeTag(tag, initialRelease);
			node.reload();
			tag.reload();

			// 6. Assert
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").isEmpty();
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release").isEmpty();

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
					.containsOnly(node);
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			InternalActionContext ac = Mocks.getMockedInternalActionContext(user());
			PageImpl<? extends Tag> tagPage = meshRoot().getTagRoot().findAll(ac, new PagingParameters(1, 10));
			assertEquals(12, tagPage.getTotalElements());
			assertEquals(10, tagPage.getSize());

			tagPage = meshRoot().getTagRoot().findAll(ac, new PagingParameters(1, 14));
			assertEquals(tags().size(), tagPage.getTotalElements());
			assertEquals(12, tagPage.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily basicTagFamily = tagFamily("basic");
			Tag noPermTag = basicTagFamily.create("noPermTag", project(), user());
			basicTagFamily.getTagRoot().addTag(noPermTag);
			assertNotNull(noPermTag.getUuid());
			assertEquals(tags().size() + 1, meshRoot().getTagRoot().findAll().size());

			PageImpl<? extends Tag> projectTagpage = project().getTagRoot().findAll(getMockedInternalActionContext(user()),
					new PagingParameters(1, 20));
			assertPage(projectTagpage, tags().size());

			PageImpl<? extends Tag> globalTagPage = meshRoot().getTagRoot().findAll(getMockedInternalActionContext(user()),
					new PagingParameters(1, 20));
			assertPage(globalTagPage, tags().size());

			role().grantPermissions(noPermTag, READ_PERM);
			globalTagPage = meshRoot().getTagRoot().findAll(getMockedInternalActionContext(user()), new PagingParameters(1, 20));
			assertPage(globalTagPage, tags().size() + 1);
		}
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
		try (NoTx noTx = db.noTx()) {
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
			root.delete(createBatch());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("car");
			Tag foundTag = meshRoot().getTagRoot().findByName("Car").toBlocking().value();
			assertNotNull(foundTag);
			assertEquals("Car", foundTag.getName());
			assertNotNull(meshRoot().getTagRoot().findByName(tag.getName()).toBlocking().value());
			assertNull("No tag with the name bogus should be found", meshRoot().getTagRoot().findByName("bogus").toBlocking().value());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("car");
			assertNotNull("The tag with the uuid could not be found", meshRoot().getTagRoot().findByUuid(tag.getUuid()).toBlocking().value());
			assertNull("A tag with the a bogus uuid should not be found but it was.",
					meshRoot().getTagRoot().findByUuid("bogus").toBlocking().value());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("basic");
			Tag tag = tagFamily.create(GERMAN_NAME, project(), user());
			assertNotNull(tag);
			String uuid = tag.getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			Tag loadedTag = meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value();
			assertNotNull("The folder could not be found.", loadedTag);
			String name = loadedTag.getName();
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
			assertEquals(10, tagFamily.getTagRoot().findAll().size());
			latch.countDown();
			Tag projectTag = tagFamily.getTagRoot().findByUuid(uuid).toBlocking().value();
			assertNotNull("The tag should also be assigned to the project tag root", projectTag);
		}

	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			List<String> languageTags = new ArrayList<>();
			languageTags.add("en");
			languageTags.add("de");
			int depth = 3;

			RoutingContext rc = getMockedRoutingContext("lang=de,en", user());
			InternalActionContext ac = InternalActionContext.create(rc);
			int nTransformations = 100;
			for (int i = 0; i < nTransformations; i++) {
				long start = System.currentTimeMillis();
				TagResponse response = tag.transformToRest(ac, 0).toBlocking().value();

				assertNotNull(response);
				long dur = System.currentTimeMillis() - start;
				log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
				JsonUtil.toJson(response);
			}
			// assertEquals(2, response.getChildTags().size());
			// assertEquals(4, response.getPerms().length);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("basic");
			Tag tag = tagFamily.create("someTag", project(), user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());
			tag.delete(createBatch());
			assertNull(meshRoot().getTagRoot().findByUuid(uuid).toBlocking().value());
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (NoTx noTx = db.noTx()) {
			TagFamily tagFamily = tagFamily("basic");
			Tag tag = tagFamily.create("someTag", project(), user());
			assertTrue(user().hasPermission(tagFamily, GraphPermission.READ_PERM));
			assertFalse(user().hasPermission(tag, GraphPermission.READ_PERM));
			getRequestUser().addCRUDPermissionOnRole(tagFamily, GraphPermission.CREATE_PERM, tag);
			assertTrue(user().hasPermission(tag, GraphPermission.READ_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("car");
			assertEquals("Car", tag.getName());
			assertNotNull(tag.getCreationTimestamp());
			assertNotNull(tag.getLastEditedTimestamp());
			assertNotNull(tag.getEditor());
			assertNotNull(tag.getCreator());
			assertNotNull(tag.getTagFamily());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			Map<String, ElementEntry> expectedEntries = new HashMap<>();
			String uuid = tag.getUuid();
			expectedEntries.put("tag", new ElementEntry(DELETE_ACTION, uuid));
			expectedEntries.put("node-with-tag", new ElementEntry(STORE_ACTION, content("concorde").getUuid(), project().getUuid(),
					project().getLatestRelease().getUuid(), ContainerType.DRAFT, "en", "de"));
			SearchQueueBatch batch = createBatch();
			tag.delete(batch);
			batch.reload();
			assertAffectedElements(expectedEntries, batch);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (NoTx noTx = db.noTx()) {
			Tag tag = tag("red");
			tag.setName("Blue");
			assertEquals("Blue", tag.getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (NoTx noTx = db.noTx()) {
			testPermission(GraphPermission.READ_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTx noTx = db.noTx()) {
			testPermission(GraphPermission.DELETE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTx noTx = db.noTx()) {
			testPermission(GraphPermission.UPDATE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTx noTx = db.noTx()) {
			testPermission(GraphPermission.CREATE_PERM, tag("red"));
		}
	}

}
