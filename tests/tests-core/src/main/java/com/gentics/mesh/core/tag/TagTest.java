package com.gentics.mesh.core.tag;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@MeshTestSetting(testSize = FULL, startServer = false)
public class TagTest extends AbstractMeshTest implements BasicObjectTestcases {

	private static Logger log = LoggerFactory.getLogger(TagTest.class);

	public static final String GERMAN_NAME = "test german name";

	public static final String ENGLISH_NAME = "test english name";

	private BranchMigration branchMigrationHandler;

	@Before
	public void setupHandler() {
		this.branchMigrationHandler = meshDagger().branchMigrationHandler();
	}

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibTag tag = tag("red");
			TagReference reference = tag.transformToReference();
			assertNotNull(reference);
			assertEquals(tag.getUuid(), reference.getUuid());
			assertEquals(tag.getName(), reference.getName());
		}
	}

	@Test
	public void testTagFamilyTagCreation() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();

			final String TAG_FAMILY_NAME = "mycustomtagFamily";
			HibTagFamily tagFamily = tagFamilyDao.create(project(), TAG_FAMILY_NAME, user());
			assertNotNull(tagFamily);
			assertEquals(TAG_FAMILY_NAME, tagFamily.getName());
			assertNull(tagFamily.getDescription());
			tagFamily.setDescription("description");
			assertEquals("description", tagFamily.getDescription());
			assertEquals(0, tagDao.computeCount(tagFamily));
			assertNotNull(tagDao.create(tagFamily, GERMAN_NAME, project(), user()));
			assertEquals(1, tagDao.computeCount(tagFamily));
		}
	}

	@Test
	public void testReadFieldContainer() {
		try (Tx tx = tx()) {
			HibTag tag = tags().get("red");
			assertEquals("red", tag.getName());
		}
	}

	@Test
	public void testSimpleTag() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTagFamily root = tagFamily("basic");
			HibTag tag = tagDao.create(root, "test", project(), user());
			assertEquals("test", tag.getName());
			tag.setName("test2");
			assertEquals("test2", tag.getName());
		}
	}

	@Test
	public void testProjectTag() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTagFamily root = tagFamily("basic");
			HibTag tag = tagDao.create(root, "test", project(), user());
			assertEquals(project(), tag.getProject());
		}
	}

	@Test
	public void testNodeTagging() throws Exception {
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
			NodeDao nodeDao = tx.nodeDao();
			TagDao tagDao = tx.tagDao();
			// 1. Create the tag
			HibTagFamily root = tagFamily("basic");
			HibProject project = project();
			HibBranch branch = project.getLatestBranch();
			HibTag tag = tagDao.create(root, ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(tx.tagDao().findByUuid(uuid));

			// 2. Create the node
			final String GERMAN_TEST_FILENAME = "german.html";
			HibNode parentNode = folder("2015");
			HibNode node = nodeDao.create(parentNode, user(), getSchemaContainer().getLatestVersion(), project);
			String german = "de";
			NodeGraphFieldContainer germanContainer = boot().contentDao().createGraphFieldContainer(node, german, branch, user());

			germanContainer.createString("displayName").setString(GERMAN_TEST_FILENAME);
			germanContainer.createString("name").setString("german node name");

			// 3. Assign the tag to the node
			tagDao.addTag(node, tag, branch);

			// 4. Reload the tag and inspect the tagged nodes
			HibTag reloadedTag = tx.tagDao().findByUuid(tag.getUuid());
			assertEquals("The tag should have exactly one node.", 1, tagDao.getNodes(reloadedTag, branch).count());
			HibNode contentFromTag = tagDao.getNodes(reloadedTag, branch).iterator().next();
			NodeGraphFieldContainer fieldContainer = contentDao.getLatestDraftFieldContainer(contentFromTag, german);

			assertNotNull(contentFromTag);
			assertEquals("We did not get the correct content.", node.getUuid(), contentFromTag.getUuid());
			String filename = fieldContainer.getString("displayName").getString();
			assertEquals("The name of the file from the loaded tag did not match the expected one.", GERMAN_TEST_FILENAME, filename);

			// Remove the file/content and check whether the content was really removed
			tagDao.removeNode(reloadedTag, contentFromTag);
			// TODO verify for removed node
			assertEquals("The tag should not have any file.", 0, tagDao.getNodes(reloadedTag, branch).count());
		}

	}

	@Test
	public void testNodeTaggingInBranch() throws Exception {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			TagDao tagDao = tx.tagDao();
			// 1. Create the tag
			HibTagFamily root = tagFamily("basic");
			HibProject project = project();
			HibTag tag = tagDao.create(root, ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(tagDao.findByUuid(uuid));
			assertNotNull(tagDao.findByUuid(project, uuid));

			// 2. Create new branch
			HibBranch initialBranch = initialBranch();
			HibBranch newBranch = createBranch("newbranch");

			// 3. Migrate nodes to new branch
			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(initialBranch);
			branchMigrationHandler.migrateBranch(context).blockingAwait();

			// 4. Create and Tag a node
			HibNode node = nodeDao.create(folder("2015"), user(), getSchemaContainer().getLatestVersion(), project);
			tagDao.addTag(node, tag, initialBranch);

			// 5. Assert
			assertThat(new ArrayList<HibTag>(tagDao.getTags(node, initialBranch).list())).as("Tags in initial Release")
				.usingElementComparatorOnFields("uuid", "name")
				.containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, initialBranch).list())).as("Nodes with tag in initial Release")
				.usingElementComparatorOnFields(
					"uuid")
				.containsOnly(node);

			assertThat(tagDao.getTags(node, newBranch).list()).as("Tags in new Branch").isEmpty();
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, newBranch).list())).as("Nodes with tag in new Branch").isEmpty();

			// 6. Tag in new branch
			tagDao.addTag(node, tag, newBranch);

			// 7. Assert again
			assertThat(new ArrayList<HibTag>(tagDao.getTags(node, initialBranch).list())).as("Tags in initial Release")
				.usingElementComparatorOnFields("uuid", "name")
				.containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, initialBranch).list())).as("Nodes with tag in initial Release")
				.usingElementComparatorOnFields(
					"uuid")
				.containsOnly(node);

			assertThat(new ArrayList<HibTag>(tagDao.getTags(node, newBranch).list())).as("Tags in new Release").usingElementComparatorOnFields("uuid", "name")
				.containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, newBranch).list())).as("Nodes with tag in new Release").usingElementComparatorOnFields("uuid")
				.containsOnly(node);
		}
	}

	@Test
	public void testMigrateTagsForBranch() throws Exception {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			// 1. Create the tag
			HibTagFamily root = tagFamily("basic");
			HibProject project = project();
			HibBranch initialBranch = project.getInitialBranch();
			HibTag tag = tagDao.create(root, ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(tx.tagDao().findByUuid(uuid));

			// 2. Create and Tag a node
			HibNode node = folder("2015");
			tagDao.removeAllTags(node, initialBranch);
			tagDao.addTag(node, tag, initialBranch);

			// 3. Create new branch
			HibBranch newBranch = createBranch("newbranch");

			// 4. Migrate nodes to new branch
			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(initialBranch);
			branchMigrationHandler.migrateBranch(context).blockingAwait();

			// 5. Assert
			assertThat(new ArrayList<HibTag>(tagDao.getTags(node, initialBranch).list())).as("Tags in initial Branch")
				.usingElementComparatorOnFields("uuid", "name").containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, initialBranch).list())).as("Nodes with tag in initial Branch").usingElementComparatorOnFields(
				"uuid").containsOnly(node);

			assertThat(new ArrayList<HibTag>(tagDao.getTags(node, newBranch).list())).as("Tags in new Branch").usingElementComparatorOnFields("uuid", "name")
				.containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, newBranch).list())).as("Nodes with tag in new Branch").usingElementComparatorOnFields("uuid")
				.containsOnly(node);
		}
	}

	@Test
	public void testNodeUntaggingInBranch() throws Exception {
		HibNode node = folder("2015");
		HibBranch initialBranch = tx(() -> initialBranch());
		HibProject project = project();
		HibBranch newBranch = null;
		HibTag tag = null;

		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			// 1. Create the tag
			HibTagFamily root = tagFamily("basic");
			initialBranch = project.getInitialBranch();
			tag = tagDao.create(root, ENGLISH_NAME, project, user());
			String uuid = tag.getUuid();
			assertNotNull(tagDao.findByUuid(root, uuid));

			// 2. Create and Tag a node
			tagDao.removeAllTags(node, initialBranch);
			tagDao.addTag(node, tag, initialBranch);

			// 3. Create new branch
			newBranch = createBranch("newbranch");

			// 4. Migrate nodes to new branch
			tx.success();
		}

		BranchMigrationContextImpl context = new BranchMigrationContextImpl();
		context.setNewBranch(newBranch);
		context.setOldBranch(initialBranch);
		branchMigrationHandler.migrateBranch(context).blockingAwait();

		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			// 5. Untag in initial branch
			tagDao.removeTag(node, tag, initialBranch);

			// 6. Assert
			assertThat(tagDao.getTags(node, initialBranch).list()).as("Tags in initial Branch").isEmpty();
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, initialBranch).list())).as("Nodes with tag in initial Branch").isEmpty();

			assertThat(new ArrayList<>(tagDao.getTags(node, newBranch).list())).as("Tags in new Branch").usingElementComparatorOnFields("uuid", "name")
				.containsOnly(tag);
			assertThat(new ArrayList<HibNode>(tagDao.getNodes(tag, newBranch).list())).as("Nodes with tag in new Branch").usingElementComparatorOnFields("uuid")
				.containsOnly(node);
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			Page<? extends HibTag> tagPage = tx.tagDao().findAll(ac, new PagingParametersImpl(1, 10L));
			assertEquals(12, tagPage.getTotalElements());
			assertEquals(10, tagPage.getSize());

			tagPage = tx.tagDao().findAll(ac, new PagingParametersImpl(1, 14L));
			assertEquals(tags().size(), tagPage.getTotalElements());
			assertEquals(12, tagPage.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			RoleDao roleDao = tx.roleDao();
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();

			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			HibTagFamily basicTagFamily = tagFamily("basic");
			long beforeCount = tagDao.computeCount(basicTagFamily);
			HibTag noPermTag = tagDao.create(basicTagFamily, "noPermTag", project(), user());
			tagFamilyDao.addTag(basicTagFamily, noPermTag);
			assertNotNull(noPermTag.getUuid());
			assertEquals(beforeCount + 1, tagDao.computeCount(basicTagFamily));

			Page<? extends HibTag> tagfamilyTagpage = tagDao.findAll(basicTagFamily, mockActionContext(), new PagingParametersImpl(1, 20L));
			assertPage(tagfamilyTagpage, beforeCount);

			roleDao.grantPermissions(role(), noPermTag, READ_PERM);
			Page<? extends HibTag> globalTagPage = tagDao.findAll(basicTagFamily, mockActionContext(), new PagingParametersImpl(1, 20L));
			assertPage(globalTagPage, beforeCount + 1);
		}
	}

	private void assertPage(Page<? extends HibTag> page, long expectedTagCount) {
		assertNotNull(page);

		int nTags = 0;
		for (HibTag tag : page) {
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
			// TODO move into OrientDB tests when available.
			TagRoot root = ((OrientDBBootstrapInitializer) boot()).meshRoot().getTagRoot();
			assertEquals(tags().size(), root.computeCount());
			HibTag tag = tag("red");
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.delete(createBulkContext());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTag tag = tag("car");
			HibTag foundTag = tagDao.findByName(tag.getTagFamily(), "Car");
			assertNotNull(foundTag);
			assertEquals("Car", foundTag.getName());
			assertNull("No tag with the name bogus should be found", tagDao.findByName(tag.getTagFamily(), "bogus"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			HibTag tag = tag("car");
			assertNotNull("The tag with the uuid could not be found", tx.tagDao().findByUuid(tag.getUuid()));
			assertNull("A tag with the a bogus uuid should not be found but it was.", tx.tagDao().findByUuid("bogus"));
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTagFamily tagFamily = tagFamily("basic");
			HibTag tag = tagDao.create(tagFamily, GERMAN_NAME, project(), user());
			assertNotNull(tag);
			String uuid = tag.getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			HibTag loadedTag = tx.tagDao().findByUuid(uuid);
			assertNotNull("The folder could not be found.", loadedTag);
			String name = loadedTag.getName();
			assertEquals("The loaded name of the folder did not match the expected one.", GERMAN_NAME, name);
			assertEquals(10, tagDao.computeCount(tagFamily));
			latch.countDown();
			HibTag projectTag = tagDao.findByUuid(tagFamily, uuid);
			assertNotNull("The tag should also be assigned to the project tag root", projectTag);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTag tag = tag("red");
			assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
			List<String> languageTags = new ArrayList<>();
			languageTags.add("en");
			languageTags.add("de");
			int depth = 3;

			InternalActionContext ac = mockActionContext("lang=de,en");
			int nTransformations = 100;
			for (int i = 0; i < nTransformations; i++) {
				long start = System.currentTimeMillis();
				TagResponse response = tagDao.transformToRestSync(tag, ac, 0);

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
			TagDao tagDao = tx.tagDao();
			HibTagFamily tagFamily = tagFamily("basic");
			HibTag tag = tagDao.create(tagFamily, "someTag", project(), user());
			String uuid = tag.getUuid();
			assertNotNull(tx.tagDao().findByUuid(uuid));
			tagDao.delete(tag, createBulkContext());
			assertNull(tx.tagDao().findByUuid(uuid));
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			UserDao userDao = tx.userDao();
			HibTagFamily tagFamily = tagFamily("basic");
			HibTag tag = tagDao.create(tagFamily, "someTag", project(), user());
			assertTrue(userDao.hasPermission(user(), tagFamily, InternalPermission.READ_PERM));
			assertFalse(userDao.hasPermission(user(), tag, InternalPermission.READ_PERM));
			userDao.inheritRolePermissions(getRequestUser(), tagFamily, tag);
			assertTrue(userDao.hasPermission(user(), tag, InternalPermission.READ_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			HibTag tag = tag("car");
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
		BulkActionContext bac = createBulkContext();
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			HibTag tag = tag("red");

			// Deletion of a tag must remove the tag from the index and update the nodes which reference the tag
			tagDao.delete(tag, bac);
		}
		// 2 = 1 tag + 1 tagged node
		assertEquals(2, bac.batch().size());
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			HibTag tag = tag("red");
			tag.setName("Blue");
			assertEquals("Blue", tag.getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.READ_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.DELETE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.UPDATE_PERM, tag("red"));
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.CREATE_PERM, tag("red"));
		}
	}

}
