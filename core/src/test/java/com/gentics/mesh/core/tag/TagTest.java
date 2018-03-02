package com.gentics.mesh.core.tag;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.test.TestSize.FULL;
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
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.migration.release.ReleaseMigrationHandler;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class TagTest extends AbstractMeshTest implements BasicObjectTestcases {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	private ReleaseMigrationHandler releaseMigrationHandler;

	@Before
	public void setupHandler() {
		this.releaseMigrationHandler = meshDagger().releaseMigrationHandler();
	}

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			TagReference reference = tag.transformToReference();
			assertNotNull(reference);
			assertEquals(tag.getUuid(), reference.getUuid());
			assertEquals(tag.getName(), reference.getName());
		}
	}

	@Test
	public void testTagFamilyTagCreation() {
		try (Tx tx = tx()) {
			final String TAG_FAMILY_NAME = "mycustomtagFamily";
			TagFamily tagFamily = project().getTagFamilyRoot().create(TAG_FAMILY_NAME, user());
			assertNotNull(tagFamily);
			assertEquals(TAG_FAMILY_NAME, tagFamily.getName());
			assertNull(tagFamily.getDescription());
			tagFamily.setDescription("description");
			assertEquals("description", tagFamily.getDescription());
			assertEquals(0, tagFamily.computeCount());
			assertNotNull(tagFamily.create(GERMAN_NAME, project(), user()));
			assertEquals(1, tagFamily.computeCount());
		}
	}

	@Test
	public void testReadFieldContainer() {
		try (Tx tx = tx()) {
			Tag tag = tags().get("red");
			assertEquals("red", tag.getName());
		}
	}

	@Test
	public void testSimpleTag() {
		try (Tx tx = tx()) {
			TagFamily root = tagFamily("basic");
			Tag tag = root.create("test", project(), user());
			assertEquals("test", tag.getName());
			tag.setName("test2");
			assertEquals("test2", tag.getName());
		}
	}

	@Test
	public void testProjectTag() {
		try (Tx tx = tx()) {
			TagFamily root = tagFamily("basic");
			Tag tag = root.create("test", project(), user());
			assertEquals(project(), tag.getProject());
		}
	}

	@Test
	public void testNodeTagging() throws Exception {
		try (Tx tx = tx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release release = project.getLatestRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid));

			// 2. Create the node
			final String GERMAN_TEST_FILENAME = "german.html";
			Node parentNode = folder("2015");
			Node node = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project);
			Language german = boot().languageRoot().findByLanguageTag("de");
			NodeGraphFieldContainer germanContainer = node.createGraphFieldContainer(german, release, user());

			germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
			germanContainer.createString("name").setString("german node name");

			// 3. Assign the tag to the node
			node.addTag(tag, release);

			// 4. Reload the tag and inspect the tagged nodes
			Tag reloadedTag = meshRoot().getTagRoot().findByUuid(tag.getUuid());
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
		try (Tx tx = tx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(root.findByUuid(uuid));

			// 2. Create new Release
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 3. Migrate nodes to new release
			releaseMigrationHandler.migrateRelease(newRelease, null);

			// 4. Create and Tag a node
			Node node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project);
			node.addTag(tag, initialRelease);

			// 5. Assert
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release").usingElementComparatorOnFields(
					"uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").isEmpty();
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").isEmpty();

			// 6. Tag in new Release
			node.addTag(tag, newRelease);

			// 7. Assert again
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release").usingElementComparatorOnFields(
					"uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
					.containsOnly(node);
		}
	}

	@Test
	public void testMigrateTagsForRelease() throws Exception {
		try (Tx tx = tx()) {
			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid));

			// 2. Create and Tag a node
			Node node = folder("2015");
			node.removeAllTags(initialRelease);
			node.addTag(tag, initialRelease);

			// 3. Create new Release
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. Migrate nodes to new release
			releaseMigrationHandler.migrateRelease(newRelease, null);

			// 5. Assert
			assertThat(new ArrayList<Tag>(node.getTags(initialRelease))).as("Tags in initial Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(initialRelease))).as("Nodes with tag in initial Release").usingElementComparatorOnFields(
					"uuid").containsOnly(node);

			assertThat(new ArrayList<Tag>(node.getTags(newRelease))).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
					.containsOnly(tag);
			assertThat(new ArrayList<Node>(tag.getNodes(newRelease))).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
					.containsOnly(node);
		}
	}

	@Test
	public void testNodeUntaggingInRelease() throws Exception {
		try (Tx tx = tx()) {
			Release initialRelease = null;
			Release newRelease = null;

			// 1. Create the tag
			TagFamily root = tagFamily("basic");
			Project project = project();
			initialRelease = project.getInitialRelease();
			Tag tag = root.create(ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(root.findByUuid(uuid));

			// 2. Create and Tag a node
			Node node = folder("2015");
			node.removeAllTags(initialRelease);
			node.addTag(tag, initialRelease);

			// 3. Create new Release
			newRelease = project.getReleaseRoot().create("newrelease", user());

			// 4. Migrate nodes to new release
			releaseMigrationHandler.migrateRelease(newRelease, null);

			// 5. Untag in initial Release
			node.removeTag(tag, initialRelease);

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
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			Page<? extends Tag> tagPage = meshRoot().getTagRoot().findAll(ac, new PagingParametersImpl(1, 10));
			assertEquals(12, tagPage.getTotalElements());
			assertEquals(10, tagPage.getSize());

			tagPage = meshRoot().getTagRoot().findAll(ac, new PagingParametersImpl(1, 14));
			assertEquals(tags().size(), tagPage.getTotalElements());
			assertEquals(12, tagPage.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily basicTagFamily = tagFamily("basic");
			long beforeCount = basicTagFamily.computeCount();
			Tag noPermTag = basicTagFamily.create("noPermTag", project(), user());
			basicTagFamily.addTag(noPermTag);
			assertNotNull(noPermTag.getUuid());
			assertEquals(beforeCount + 1, basicTagFamily.computeCount());

			Page<? extends Tag> tagfamilyTagpage = basicTagFamily.findAll(mockActionContext(), new PagingParametersImpl(1, 20));
			assertPage(tagfamilyTagpage, beforeCount);

			role().grantPermissions(noPermTag, READ_PERM);
			Page<? extends Tag> globalTagPage = basicTagFamily.findAll(mockActionContext(), new PagingParametersImpl(1, 20));
			assertPage(globalTagPage, beforeCount + 1);
		}
	}

	private void assertPage(Page<? extends Tag> page, long expectedTagCount) {
		assertNotNull(page);

		int nTags = 0;
		for (Tag tag : page) {
			assertNotNull(tag.getName());
			nTags++;
		}
		assertEquals("The page did not contain the correct amount of tags", expectedTagCount, nTags);
		assertEquals(expectedTagCount, page.getTotalElements());
		assertEquals(1, page.getNumber());
		assertEquals(1, page.getPageCount());

	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			TagRoot root = meshRoot().getTagRoot();
			assertEquals(tags().size(), root.computeCount());
			Tag tag = tag("red");
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.delete(createBatch());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			Tag tag = tag("car");
			Tag foundTag = tag.getTagFamily().findByName("Car");
			assertNotNull(foundTag);
			assertEquals("Car", foundTag.getName());
			assertNull("No tag with the name bogus should be found", tag.getTagFamily().findByName("bogus"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			Tag tag = tag("car");
			assertNotNull("The tag with the uuid could not be found", meshRoot().getTagRoot().findByUuid(tag.getUuid()));
			assertNull("A tag with the a bogus uuid should not be found but it was.", meshRoot().getTagRoot().findByUuid("bogus"));
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("basic");
			Tag tag = tagFamily.create(GERMAN_NAME, project(), user());
			assertNotNull(tag);
			String uuid = tag.getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			Tag loadedTag = meshRoot().getTagRoot().findByUuid(uuid);
			assertNotNull("The folder could not be found.", loadedTag);
			String name = loadedTag.getName();
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
			assertEquals(10, tagFamily.computeCount());
			latch.countDown();
			Tag projectTag = tagFamily.findByUuid(uuid);
			assertNotNull("The tag should also be assigned to the project tag root", projectTag);
		}

	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			List<String> languageTags = new ArrayList<>();
			languageTags.add("en");
			languageTags.add("de");
			int depth = 3;

			InternalActionContext ac = mockActionContext("lang=de,en");
			int nTransformations = 100;
			for (int i = 0; i < nTransformations; i++) {
				long start = System.currentTimeMillis();
				TagResponse response = tag.transformToRest(ac, 0).blockingGet();

				assertNotNull(response);
				long dur = System.currentTimeMillis() - start;
				log.info("Transformation with depth {" + depth + "} took {" + dur + "} [ms]");
				response.toJson();
			}
			// assertEquals(2, response.getChildTags().size());
			// assertEquals(4, response.getPerms().length);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			TagFamily tagFamily = tagFamily("basic");
			Tag tag = tagFamily.create("someTag", project(), user());
			String uuid = tag.getUuid();
			assertNotNull(meshRoot().getTagRoot().findByUuid(uuid));
			tag.delete(createBatch());
			assertNull(meshRoot().getTagRoot().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			Map<String, ElementEntry> expectedEntries = new HashMap<>();
			String uuid = tag.getUuid();

			// Deletion of a tag must remove the tag from the index and update the nodes which reference the tag
			expectedEntries.put("tag", new ElementEntry(DELETE_ACTION, uuid));
			expectedEntries.put("node-with-tag", new ElementEntry(STORE_ACTION, content("concorde").getUuid(), project().getUuid(), project()
					.getLatestRelease().getUuid(), ContainerType.DRAFT));
			SearchQueueBatch batch = createBatch();
			tag.delete(batch);
			assertThat(batch).containsEntries(expectedEntries);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			Tag tag = tag("red");
			tag.setName("Blue");
			assertEquals("Blue", tag.getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.READ_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.DELETE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.UPDATE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.CREATE_PERM, tag("red"));
		}
	}

}
