package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.SortOrder.UNSORTED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.TestUtils.size;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.context.impl.LocalActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.test.util.TestUtils;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = content();
			InternalActionContext ac = mockActionContext("?version=draft");
			NodeReference reference = tx.nodeDao().transformToReference(node, ac);
			assertNotNull(reference);
			assertEquals(node.getUuid(), reference.getUuid());
		}
	}

	@Test
	public void testGetPath() throws Exception {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			ContentDao contentDao = tx.contentDao();

			HibNode newsNode = content("news overview");
			InternalActionContext ac = mockActionContext();
			String path = nodeDao.getPath(newsNode, ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, english());
			assertEquals("/News/News%20Overview.en.html", path);
			String pathSegementFieldValue = contentDao.getPathSegment(newsNode, project().getLatestBranch().getUuid(), ContainerType.DRAFT,
				english());
			assertEquals("News Overview.en.html", pathSegementFieldValue);
		}
	}

	@Test
	public void testMeshNodeStructure() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibNode newsNode = content("news overview");
			assertNotNull(newsNode);
			HibNode newSubNode;
			newSubNode = nodeDao.create(newsNode, user(), getSchemaContainer().getLatestVersion(), project());

			assertEquals(1, size(nodeDao.getChildren(newsNode)));
			HibNode firstChild = nodeDao.getChildren(newsNode).iterator().next();
			assertEquals(newSubNode.getUuid(), firstChild.getUuid());
		}
	}

	@Test
	public void testTaggingOfMeshNode() {
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();

			HibNode newsNode = content("news overview");
			assertNotNull(newsNode);

			HibTag carTag = tag("car");
			assertNotNull(carTag);

			tagDao.addTag(newsNode, carTag, project().getLatestBranch());

			assertEquals(1, tagDao.getTags(newsNode, project().getLatestBranch()).count());
			HibTag firstTag = tagDao.getTags(newsNode, project().getLatestBranch()).iterator().next();
			assertEquals(carTag.getUuid(), firstTag.getUuid());
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			InternalActionContext ac = mockActionContext("version=draft");
			Page<? extends HibNode> page = nodeDao.findAll(project(), ac, new PagingParametersImpl(1, 10L));

			assertEquals(getNodeCount(), page.getTotalElements());
			assertEquals(10, page.getSize());

			page = nodeDao.findAll(project(), ac, new PagingParametersImpl(1, 15L));
			assertEquals(getNodeCount(), page.getTotalElements());
			assertEquals(15, page.getSize());
		}
	}

	@Test
	public void testMeshNodeFields() throws IOException {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			NodeDao nodeDao = tx.nodeDao();
			HibNode newsNode = content("news overview");
			String german = german();
			InternalActionContext ac = mockActionContext("lang=de,en&version=draft");
			assertThat(ac.getNodeParameters().getLanguages()).containsExactly("de", "en");
			HibNodeFieldContainer germanFields = contentDao.getLatestDraftFieldContainer(newsNode, german);
			String expectedDisplayName = germanFields.getString(newsNode.getSchemaContainer().getLatestVersion().getSchema().getDisplayField())
				.getString();
			assertEquals("The display name value did not match up", expectedDisplayName, nodeDao.getDisplayName(newsNode, ac));
			// TODO add some fields
		}
	}

	@Test
	@Override
	@Ignore("No need to cover this by test")
	public void testFindAllVisible() throws InvalidArgumentException {
		// try (Tx tx = tx()) {
		// List<String> languageTags = new ArrayList<>();
		// languageTags.add("de");
		// languageTags.add("en");
		// Page<? extends Node> page = project().getNodeRoot().findAll(getRequestUser(), languageTags, new PagingParametersImpl(1, 25L));
		// assertNotNull(page);
		// }
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			HibProject project = project();
			HibNode root = project.getBaseNode();
			assertNotNull(root);
		}
	}

	@Test
	@Override
	@Ignore("nodes can not be located using the name")
	public void testFindByName() {

	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			HibNode newsNode = content("news overview");
			HibNode node = boot().nodeDao().findByUuid(project(), newsNode.getUuid());
			assertNotNull(node);
			assertEquals(newsNode.getUuid(), node.getUuid());
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			InternalActionContext ac = mockActionContext("lang=en&version=draft");
			HibNode newsNode = content("concorde");

			NodeResponse response = nodeDao.transformToRestSync(newsNode, ac, 0);
			String json = response.toJson();
			assertNotNull(json);

			NodeResponse deserialized = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(deserialized);

			assertThat(deserialized).as("node response").hasVersion("1.0");

			assertThat(deserialized.getCreator()).as("Creator").isNotNull();
			assertThat(deserialized.getCreated()).as("Created").isNotEqualTo(0);
			assertThat(deserialized.getEditor()).as("Editor").isNotNull();
			assertThat(deserialized.getEdited()).as("Edited").isNotEqualTo(0);

			// TODO assert for english fields
		}
	}

	@Test
	@Override
	public void testCreateDelete() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibNode folder = folder("2015");
			HibNode subNode = nodeDao.create(folder, user(), getSchemaContainer().getLatestVersion(), project());
			assertNotNull(subNode.getUuid());
			BulkActionContext context = createBulkContext();
			InternalActionContext ac = mockActionContext("");
			boot().nodeDao().deleteFromBranch(subNode, ac, project().getLatestBranch(), context, false);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			UserDao userDao = tx.userDao();
			HibNode node = nodeDao.create(folder("2015"), user(), getSchemaContainer().getLatestVersion(), project());
			InternalActionContext ac = mockActionContext("");
			assertFalse(userDao.hasPermission(user(), node, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user(), folder("2015"), node);
			ac.data().clear();
			assertTrue(userDao.hasPermission(user(), node, InternalPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() throws IOException {
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			assertEquals("folder", node.getSchemaContainer().getLatestVersion().getSchema().getName());
			assertTrue(node.getSchemaContainer().getLatestVersion().getSchema().getContainer());
			HibNodeFieldContainer englishVersion = boot().contentDao().getFieldContainer(node, "en");
			assertNotNull(englishVersion);
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			NodeDao nodeDao = tx.nodeDao();
			HibUser user = user();
			HibNode parentNode = folder("2015");
			HibSchemaVersion schemaVersion = schemaContainer("content").getLatestVersion();
			HibNode node = nodeDao.create(parentNode, user, schemaVersion, project());
			long ts = System.currentTimeMillis();
			node.setCreationTimestamp(ts);
			Long creationTimeStamp = node.getCreationTimestamp();
			assertNotNull(creationTimeStamp);
			assertEquals(ts, creationTimeStamp.longValue());
			assertEquals(user.getUuid(), node.getCreator().getUuid());
			String english = english();
			String german = german();

			HibNodeFieldContainer englishContainer = boot().contentDao().createFieldContainer(node, english,
				node.getProject().getLatestBranch(), user);
			schemaVersion.getSchema().addField(new StringFieldSchemaImpl().setName("name"));
			actions().updateSchemaVersion(schemaVersion);
			englishContainer.createHTML("content").setHtml("english content");
			englishContainer.createString("name").setString("english.html");
			assertNotNull(node.getUuid());
			assertEquals(user.getUuid(), englishContainer.getEditor().getUuid());
			assertNotNull(englishContainer.getLastEditedTimestamp());

			List<HibNodeFieldContainer> allProperties = TestUtils.toList(contentDao.getDraftFieldContainers(node));
			assertNotNull(allProperties);
			assertEquals(1, allProperties.size());

			HibNodeFieldContainer germanContainer = boot().contentDao().createFieldContainer(node, german, node.getProject().getLatestBranch(),
				user);
			germanContainer.createHTML("content").setHtml("german content");
			assertEquals(2, TestUtils.size(contentDao.getDraftFieldContainers(node)));

			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english);
			assertNotNull(container);
			String text = container.getHtml("content").getHTML();
			assertNotNull(text);
			assertEquals("english content", text);
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = Tx.get().nodeDao().findByUuidGlobal(folder("news").getUuid());
			String uuid = node.getUuid();
			MeshAssert.assertElement(tx.nodeDao(), project(), uuid, true);
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			try (Tx tx2 = tx()) {
				boot().nodeDao().deleteFromBranch(node, ac, project().getLatestBranch(), createBulkContext(), false);
				tx2.success();
			}

			MeshAssert.assertElement(tx.nodeDao(), project(), uuid, false);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			HibNode node = content();
			try (Tx tx2 = tx()) {
				UserDao userDao = tx2.userDao();
				HibUser newUser = userDao.create("newUser", user());
				userDao.addGroup(newUser, group());
				assertEquals(user().getUuid(), node.getCreator().getUuid());
				System.out.println(newUser.getUuid());
				node.setCreator(newUser);
				System.out.println(node.getCreator().getUuid());

				assertEquals(newUser.getUuid(), node.getCreator().getUuid());
				// TODO update other fields
			}
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.READ_PERM, content());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.DELETE_PERM, content());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.UPDATE_PERM, content());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.CREATE_PERM, content());
		}
	}

	@Test
	public void testDeleteWithChildren() {
		BulkActionContext bac = createBulkContext();
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = project();
			HibBranch initialBranch = project.getInitialBranch();
			HibSchemaVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder with subfolder and subsubfolder
			HibNode folder = nodeDao.create(project.getBaseNode(), user(), folderSchema, project);
			boot().contentDao().createFieldContainer(folder, english(), initialBranch, user()).createString("name").setString("Folder");
			String folderUuid = folder.getUuid();
			HibNode subFolder = nodeDao.create(folder, user(), folderSchema, project);
			boot().contentDao().createFieldContainer(subFolder, english(), initialBranch, user()).createString("name").setString("SubFolder");
			String subFolderUuid = subFolder.getUuid();
			HibNode subSubFolder = nodeDao.create(subFolder, user(), folderSchema, project);
			boot().contentDao().createFieldContainer(subSubFolder, english(), initialBranch, user()).createString("name")
				.setString("SubSubFolder");
			String subSubFolderUuid = subSubFolder.getUuid();

			// 2. delete folder for initial release
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			boot().nodeDao().deleteFromBranch(subFolder, ac, initialBranch, bac, false);

			// 3. assert for new branch
			assertThat(folder).as("folder").hasNoChildren(initialBranch);

			// 4. assert for initial branch
			List<String> nodeUuids = new ArrayList<>();
			nodeDao.findAll(project).forEach(node -> nodeUuids.add(node.getUuid()));
			assertThat(nodeUuids).as("All nodes").contains(folderUuid).doesNotContain(subFolderUuid, subSubFolderUuid);
		}
	}

	@Test
	public void testDeleteWithChildrenInBranch() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			HibBranch initialBranch = reloadBranch(initialBranch());
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			NodeDao nodeDao = tx.nodeDao();
			BulkActionContext bac = createBulkContext();
			HibSchemaVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder with subfolder and subsubfolder
			HibNode folder = nodeDao.create(project.getBaseNode(), user(), folderSchema, project);
			boot().contentDao().createFieldContainer(folder, english(), initialBranch, user()).createString("name").setString("Folder");
			HibNode subFolder = nodeDao.create(folder, user(), folderSchema, project);
			boot().contentDao().createFieldContainer(subFolder, english(), initialBranch, user()).createString("name").setString("SubFolder");
			String subFolderUuid = subFolder.getUuid();
			HibNode subSubFolder = nodeDao.create(subFolder, user(), folderSchema, project);
			boot().contentDao().createFieldContainer(subSubFolder, english(), initialBranch, user()).createString("name")
				.setString("SubSubFolder");
			String subSubFolderUuid = subSubFolder.getUuid();

			// 2. create a new branch
			HibBranch newBranch = createBranch("newbranch");

			// 3. migrate nodes
			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(initialBranch);
			meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

			// 4. assert nodes in new branch
			assertThat(folder).as("folder").hasOnlyChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasOnlyChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasNoChildren(newBranch);

			EventQueueBatch batch = createBatch();
			// 5. reverse folders in new branch
			nodeDao.moveTo(subSubFolder, mockActionContext(), folder, batch);
			nodeDao.moveTo(subFolder, mockActionContext(), subSubFolder, batch);

			// 6. assert for new branch
			assertThat(folder).as("folder").hasChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasNoChildren(newBranch);

			// 7. assert for initial branch
			assertThat(folder).as("folder").hasChildren(initialBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasChildren(initialBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasNoChildren(initialBranch);

			// 8. delete folder for initial release
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			boot().nodeDao().deleteFromBranch(subFolder, ac, initialBranch, bac, false);

			// 9. assert for new branch
			assertThat(folder).as("folder").hasChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasNoChildren(newBranch);

			// 10. assert for initial branch
			List<HibNode> nodes = new ArrayList<>();
			nodeDao.findAll(project, mockActionContext("release=" + initialBranch.getName()), new PagingParametersImpl(1, 10000L, "name",
				SortOrder.ASCENDING)).forEach(node -> nodes.add(node));
			assertThat(nodes).as("Nodes in initial branch").usingElementComparatorOnFields("uuid").doesNotContain(subFolder, subSubFolder);
			assertThat(folder).as("folder").hasNoChildren(initialBranch);

		}
	}

	@Test
	public void testDeletePublished() throws InvalidArgumentException {
		HibProject project = project();
		BulkActionContext bac = createBulkContext();

		try (Tx tx = tx()) {
			HibBranch initialBranch = reloadBranch(initialBranch());
			HibSchemaVersion folderSchema = Tx.get().schemaDao().findByUuid(schemaContainer("folder").getUuid()).getLatestVersion();

			// 1. create folder and publish
			String folderUuid = tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();
				RoleDao roleDao = tx2.roleDao();
				HibNode folder = nodeDao.create(project.getBaseNode(), user(), folderSchema, project);
				BulkActionContext bac2 = createBulkContext();
				roleDao.applyPermissions(folder, bac.batch(), role(), false, new HashSet<>(Arrays.asList(InternalPermission.READ_PERM,
					InternalPermission.READ_PUBLISHED_PERM)), Collections.emptySet());
				boot().contentDao().createFieldContainer(folder, english(), initialBranch, user()).createString("name").setString("Folder");
				nodeDao.publish(folder, mockActionContext(), bac2);
				assertEquals(1, bac2.batch().size());
				return folder.getUuid();
			});

			// 2. assert published and draft node
			tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();

				List<String> nodeUuids = new ArrayList<>();
				nodeDao.findAll(project, mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").contains(folderUuid);
				nodeUuids.clear();
				nodeDao.findAll(project, mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").contains(folderUuid);
			});

			// 3. delete
			InternalActionContext ac = mockActionContext("");
			tx(tx2 -> {
				tx2.nodeDao().deleteFromBranch(boot().nodeDao().findByUuid(project(), folderUuid), ac, initialBranch, bac, false);
			});

			// 4. assert published and draft gone
			tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();
				List<String> nodeUuids = new ArrayList<>();
				nodeDao.findAll(project, mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").doesNotContain(folderUuid);

				nodeUuids.clear();
				nodeDao.findAll(project, mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").doesNotContain(folderUuid);
			});
		}
	}

	@Test
	public void testDeletePublishedFromBranch() {
		try (Tx tx = tx()) {
			HibProject project = project();
			HibBranch initialBranch = project.getInitialBranch();
			HibSchemaVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder and publish
			String folderUuid = tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();
				HibNode folder = nodeDao.create(project.getBaseNode(), user(), folderSchema, project);
				BulkActionContext bac = createBulkContext();
				RoleDao roleDao = tx.roleDao();
				roleDao.applyPermissions(folder, bac.batch(), role(), false, new HashSet<>(Arrays.asList(InternalPermission.READ_PERM,
					InternalPermission.READ_PUBLISHED_PERM)), Collections.emptySet());
				boot().contentDao().createFieldContainer(folder, english(), initialBranch, user()).createString("name").setString("Folder");
				nodeDao.publish(folder, mockActionContext(), bac);
				return folder.getUuid();
			});

			// 2. create new branch and migrate nodes
			HibBranch newBranch = tx(() -> createBranch("newbranch"));
			mesh().branchCache().clear();

			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(initialBranch);
			meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();
			// 3. delete from initial branch
			InternalActionContext ac = mockActionContext("");
			tx(() -> {
				boot().nodeDao().deleteFromBranch(boot().nodeDao().findByUuid(project(), folderUuid), ac, initialBranch, createBulkContext(),
					false);
			});

			// 4. assert published and draft gone from initial branch
			tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();
				List<String> nodeUuids = new ArrayList<>();
				nodeDao.findAll(project, mockActionContext("version=draft&branch=" + initialBranch.getUuid()), new PagingParametersImpl(1,
					10000L, null, UNSORTED)).forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").doesNotContain(folderUuid);

				nodeUuids.clear();
				nodeDao.findAll(project, mockActionContext("version=published&branch=" + initialBranch.getUuid()), new PagingParametersImpl(1,
					10000L, null, UNSORTED)).forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").doesNotContain(folderUuid);
			});

			// 5. assert published and draft still there for new branch
			tx(tx2 -> {
				NodeDao nodeDao = tx2.nodeDao();
				List<String> nodeUuids = new ArrayList<>();
				nodeDao.findAll(project, mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, UNSORTED)).forEach(
					node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").contains(folderUuid);

				nodeUuids.clear();
				nodeDao.findAll(project, mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, UNSORTED)).forEach(
					node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").contains(folderUuid);
			});

		}
	}

	/**
	 * Test implementation of {@link NodeDao#getChildrenStream(HibNode, InternalActionContext)}}.
	 * Check whether
	 * <ol>
	 * <li>All children in the given branch are returned</li>
	 * <li>Permissions are checked</li>
	 * </ol>
	 */
	@Test
	public void testGetChildrenStream() {
		String initialBranchUuid = tx(() -> project().getInitialBranch().getUuid());
		String newsFolderUuid = tx(() -> folder("news").getUuid());

		createChild(newsFolderUuid, "in both branches", initialBranchUuid);

		String newBranchUuid = tx(() -> {
			HibBranch newBranch = createBranch("newbranch");

			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(newBranch);
			context.setOldBranch(newBranch.getPreviousBranch());
			meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

			return newBranch.getUuid();
		});

		createChild(newsFolderUuid, "in initial branch", initialBranchUuid);
		createChild(newsFolderUuid, "in initial branch - no read", initialBranchUuid, InternalPermission.READ_PERM);
		createChild(newsFolderUuid, "in initial branch - no read_published", initialBranchUuid, InternalPermission.READ_PUBLISHED_PERM);
		createChild(newsFolderUuid, "in initial branch - no read, read_published", initialBranchUuid, InternalPermission.READ_PERM, InternalPermission.READ_PUBLISHED_PERM);

		createChild(newsFolderUuid, "in new branch", newBranchUuid);
		createChild(newsFolderUuid, "in new branch - no read", newBranchUuid, InternalPermission.READ_PERM);
		createChild(newsFolderUuid, "in new branch - no read_published", newBranchUuid, InternalPermission.READ_PUBLISHED_PERM);
		createChild(newsFolderUuid, "in new branch - no read, read_published", newBranchUuid, InternalPermission.READ_PERM, InternalPermission.READ_PUBLISHED_PERM);

		assertThat(getChildrenNames(initialBranchUuid)).as("Children in initial branch").doesNotHaveDuplicates()
				.containsOnly("2014", "2015", "News Overview english title", "in both branches", "in initial branch",
						"in initial branch - no read", "in initial branch - no read_published");
		assertThat(getChildrenNames(newBranchUuid)).as("Children in new branch").doesNotHaveDuplicates().containsOnly(
				"2014", "2015", "News Overview english title", "in both branches", "in new branch",
				"in new branch - no read", "in new branch - no read_published");

		grantAdmin();

		assertThat(getChildrenNames(initialBranchUuid)).as("Children in initial branch for admin")
				.doesNotHaveDuplicates().containsOnly("2014", "2015", "News Overview english title", "in both branches",
						"in initial branch", "in initial branch - no read", "in initial branch - no read_published",
						"in initial branch - no read, read_published");
		assertThat(getChildrenNames(newBranchUuid)).as("Children in new branch for admin").doesNotHaveDuplicates()
				.containsOnly("2014", "2015", "News Overview english title", "in both branches", "in new branch",
						"in new branch - no read", "in new branch - no read_published",
						"in new branch - no read, read_published");
	}

	/**
	 * Get display names of the children of folder "news" in the given branch
	 * @param branchUuid branch UUID
	 * @return display names of branch children
	 */
	protected List<String> getChildrenNames(String branchUuid) {
		return tx(tx -> {
			MeshAuthUser user = tx.userDao().findMeshAuthUserByUsername(user().getUsername());
			LocalActionContextImpl<RoleResponse> ac = new LocalActionContextImpl<>(boot(), user, RoleResponse.class,
					new VersioningParametersImpl().setBranch(branchUuid));
			ac.setProject(project().getName());
			return tx.nodeDao().getChildrenStream(folder("news"), ac).map(node -> node.getDisplayName(ac))
					.collect(Collectors.toList());
		});
	}

	/**
	 * Create a child node in the given parent. Optionally revoke some permissions on the new node.
	 * @param parentUuid parent node UUID
	 * @param name name
	 * @param branchUuid branch UUID
	 * @param revoke optional list of permission to revoke
	 */
	protected void createChild(String parentUuid, String name, String branchUuid, InternalPermission...revoke) {
		String projectName = tx(() -> project().getName());
		String nodeUuid = call(() -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest()
					.setParentNode(new NodeReference().setUuid(parentUuid))
					.setSchema(new SchemaReferenceImpl().setName("folder"))
					.setLanguage("en");
			nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString(name));
			return client().createNode(projectName, nodeCreateRequest, new VersioningParametersImpl().setBranch(branchUuid));
		}).getUuid();

		if (revoke.length > 0) {
			tx(tx -> {
				RoleDao roleDao = tx.roleDao();
				HibNode node = tx.nodeDao().findByUuid(project(), nodeUuid);
				for (InternalPermission perm : revoke) {
					roleDao.revokePermissions(role(), node, perm);
				}
			});
		}
	}
}
