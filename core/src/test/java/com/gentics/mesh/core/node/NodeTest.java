package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.rest.SortOrder.UNSORTED;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NodeTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			Node node = content();
			InternalActionContext ac = mockActionContext("?version=draft");
			NodeReference reference = node.transformToReference(ac);
			assertNotNull(reference);
			assertEquals(node.getUuid(), reference.getUuid());
		}
	}

	@Test
	public void testGetPath() throws Exception {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			InternalActionContext ac = mockActionContext();
			String path = newsNode.getPath(ac, project().getLatestBranch().getUuid(), ContainerType.DRAFT, english());
			assertEquals("/News/News%20Overview.en.html", path);
			String pathSegementFieldValue = newsNode.getPathSegment(project().getLatestBranch().getUuid(), ContainerType.DRAFT, english());
			assertEquals("News Overview.en.html", pathSegementFieldValue);
		}
	}

	@Test
	public void testMeshNodeStructure() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			assertNotNull(newsNode);
			Node newSubNode;
			newSubNode = newsNode.create(user(), getSchemaContainer().getLatestVersion(), project());

			assertEquals(1, size(newsNode.getChildren()));
			Node firstChild = newsNode.getChildren().iterator().next();
			assertEquals(newSubNode.getUuid(), firstChild.getUuid());
		}
	}

	@Test
	public void testTaggingOfMeshNode() {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			assertNotNull(newsNode);

			Tag carTag = tag("car");
			assertNotNull(carTag);

			newsNode.addTag(carTag, project().getLatestBranch());

			assertEquals(1, newsNode.getTags(project().getLatestBranch()).count());
			Tag firstTag = newsNode.getTags(project().getLatestBranch()).iterator().next();
			assertEquals(carTag.getUuid(), firstTag.getUuid());
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext("version=draft");
			Page<? extends Node> page = boot().nodeRoot().findAll(ac, new PagingParametersImpl(1, 10L));

			assertEquals(getNodeCount(), page.getTotalElements());
			assertEquals(10, page.getSize());

			page = boot().nodeRoot().findAll(ac, new PagingParametersImpl(1, 15L));
			assertEquals(getNodeCount(), page.getTotalElements());
			assertEquals(15, page.getSize());
		}
	}

	@Test
	public void testMeshNodeFields() throws IOException {
		try (Tx tx = tx()) {
			Node newsNode = content("news overview");
			String german = german();
			InternalActionContext ac = mockActionContext("lang=de,en&version=draft");
			assertThat(ac.getNodeParameters().getLanguages()).containsExactly("de", "en");
			NodeGraphFieldContainer germanFields = newsNode.getLatestDraftFieldContainer(german);
			String expectedDisplayName = germanFields.getString(newsNode.getSchemaContainer().getLatestVersion().getSchema().getDisplayField())
				.getString();
			assertEquals("The display name value did not match up", expectedDisplayName, newsNode.getDisplayName(ac));
			// TODO add some fields
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			List<String> languageTags = new ArrayList<>();
			languageTags.add("de");
			languageTags.add("en");
			Page<? extends Node> page = boot().nodeRoot().findAll(getRequestUser(), languageTags, new PagingParametersImpl(1, 25L));
			assertNotNull(page);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			Project project = project();
			Node root = project.getBaseNode();
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
			Node newsNode = content("news overview");
			Node node = boot().nodeRoot().findByUuid(newsNode.getUuid());
			assertNotNull(node);
			assertEquals(newsNode.getUuid(), node.getUuid());
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext("lang=en&version=draft");
			Node newsNode = content("concorde");

			NodeResponse response = newsNode.transformToRest(ac, 0).blockingGet();
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
			Node folder = folder("2015");
			Node subNode = folder.create(user(), getSchemaContainer().getLatestVersion(), project());
			assertNotNull(subNode.getUuid());
			BulkActionContext context = createBulkContext();
			InternalActionContext ac = mockActionContext("");
			subNode.deleteFromBranch(ac, project().getLatestBranch(), context, false);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			Node node = folder("2015").create(user(), getSchemaContainer().getLatestVersion(), project());
			InternalActionContext ac = mockActionContext("");
			assertFalse(user().hasPermission(node, GraphPermission.CREATE_PERM));
			user().addCRUDPermissionOnRole(folder("2015"), GraphPermission.CREATE_PERM, node);
			ac.data().clear();
			assertTrue(user().hasPermission(node, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() throws IOException {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			assertEquals("folder", node.getSchemaContainer().getLatestVersion().getSchema().getName());
			assertTrue(node.getSchemaContainer().getLatestVersion().getSchema().isContainer());
			NodeGraphFieldContainer englishVersion = node.getGraphFieldContainer("en");
			assertNotNull(englishVersion);
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (Tx tx = tx()) {
			User user = user();
			Node parentNode = folder("2015");
			SchemaContainerVersion schemaVersion = schemaContainer("content").getLatestVersion();
			Node node = parentNode.create(user, schemaVersion, project());
			long ts = System.currentTimeMillis();
			node.setCreationTimestamp(ts);
			Long creationTimeStamp = node.getCreationTimestamp();
			assertNotNull(creationTimeStamp);
			assertEquals(ts, creationTimeStamp.longValue());
			assertEquals(user, node.getCreator());
			String english = english();
			String german = german();

			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
			englishContainer.createString("content").setString("english content");
			englishContainer.createString("name").setString("english.html");
			assertNotNull(node.getUuid());
			assertEquals(user, englishContainer.getEditor());
			assertNotNull(englishContainer.getLastEditedTimestamp());

			List<? extends GraphFieldContainer> allProperties = TestUtils.toList(node.getDraftGraphFieldContainers());
			assertNotNull(allProperties);
			assertEquals(1, allProperties.size());

			NodeGraphFieldContainer germanContainer = node.createGraphFieldContainer(german, node.getProject().getLatestBranch(), user);
			germanContainer.createString("content").setString("german content");
			assertEquals(2, TestUtils.size(node.getDraftGraphFieldContainers()));

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english);
			assertNotNull(container);
			String text = container.getString("content").getString();
			assertNotNull(text);
			assertEquals("english content", text);
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			Map<String, ElementEntry> affectedElements = new HashMap<>();
			String uuid;
			Node node = folder("news");

			// Add subfolders
			affectedElements.put("folder: news", new ElementEntry(DELETE_ACTION, node.getUuid(), "en", "de"));
			affectedElements.put("folder: news.2015", new ElementEntry(DELETE_ACTION, folder("2015").getUuid(), "en"));
			affectedElements.put("folder: news 2014", new ElementEntry(DELETE_ACTION, folder("2014").getUuid(), "en"));
			affectedElements.put("folder: news.2014.march", new ElementEntry(DELETE_ACTION, folder("march").getUuid(), "en", "de"));

			// Add Contents
			affectedElements.put("content: news.2014.news_2014", new ElementEntry(DELETE_ACTION, content("news_2014").getUuid(), "en", "de"));
			affectedElements.put("content: news.overview", new ElementEntry(DELETE_ACTION, content("news overview").getUuid(), "en", "de"));
			affectedElements.put("content: news.2014.march.news_in_march", new ElementEntry(DELETE_ACTION, content("new_in_march_2014").getUuid(),
				"en", "de"));
			affectedElements.put("content: news.2014.special_news", new ElementEntry(DELETE_ACTION, content("special news_2014").getUuid(), "en",
				"de"));
			affectedElements.put("content: news.2015.news_2015", new ElementEntry(DELETE_ACTION, content("news_2015").getUuid(), "en", "de"));

			uuid = node.getUuid();
			MeshAssert.assertElement(meshRoot().getNodeRoot(), uuid, true);
			BulkActionContext context = createBulkContext();
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			try (Tx tx2 = tx()) {
				node.deleteFromBranch(ac, project().getLatestBranch(), context, false);
				tx2.success();
			}

			MeshAssert.assertElement(meshRoot().getNodeRoot(), uuid, false);
			assertThat(context.batch()).containsEntries(affectedElements);
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			Node node = content();
			try (Tx tx2 = tx()) {
				User newUser = meshRoot().getUserRoot().create("newUser", user());
				newUser.addGroup(group());
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
			testPermission(GraphPermission.READ_PERM, content());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.DELETE_PERM, content());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.UPDATE_PERM, content());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.CREATE_PERM, content());
		}
	}

	@Test
	public void testDeleteWithChildren() {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			SchemaContainerVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder with subfolder and subsubfolder
			Node folder = project.getBaseNode().create(user(), folderSchema, project);
			folder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("Folder");
			String folderUuid = folder.getUuid();
			Node subFolder = folder.create(user(), folderSchema, project);
			subFolder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("SubFolder");
			String subFolderUuid = subFolder.getUuid();
			Node subSubFolder = subFolder.create(user(), folderSchema, project);
			subSubFolder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("SubSubFolder");
			String subSubFolderUuid = subSubFolder.getUuid();

			// 2. delete folder for initial release
			BulkActionContext context = createBulkContext();
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			subFolder.deleteFromBranch(ac, initialBranch, context, false);

			// 3. assert for new branch
			assertThat(folder).as("folder").hasNoChildren(initialBranch);

			// 4. assert for initial branch
			List<String> nodeUuids = new ArrayList<>();
			project.getNodeRoot().findAll().forEach(node -> nodeUuids.add(node.getUuid()));
			assertThat(nodeUuids).as("All nodes").contains(folderUuid).doesNotContain(subFolderUuid, subSubFolderUuid);

			// 5. assert searchqueuebatch
			Map<String, ElementEntry> affectedElements = new HashMap<>();
			affectedElements.put("subFolder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, subFolderUuid, project.getUuid(), initialBranch
				.getUuid(), ContainerType.DRAFT, "en"));
			affectedElements.put("subSubFolder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, subSubFolderUuid, project.getUuid(),
				initialBranch.getUuid(), ContainerType.DRAFT, "en"));
			assertThat(context.batch()).containsEntries(affectedElements);
		}
	}

	@Test
	public void testDeleteWithChildrenInBranch() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			SchemaContainerVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder with subfolder and subsubfolder
			Node folder = project.getBaseNode().create(user(), folderSchema, project);
			folder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("Folder");
			Node subFolder = folder.create(user(), folderSchema, project);
			subFolder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("SubFolder");
			String subFolderUuid = subFolder.getUuid();
			Node subSubFolder = subFolder.create(user(), folderSchema, project);
			subSubFolder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("SubSubFolder");
			String subSubFolderUuid = subSubFolder.getUuid();

			// 2. create a new branch
			Branch newBranch = project.getBranchRoot().create("newbranch", user());

			// 3. migrate nodes
			meshDagger().branchMigrationHandler().migrateBranch(newBranch, null).blockingAwait();

			// 4. assert nodes in new branch
			assertThat(folder).as("folder").hasOnlyChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasOnlyChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasNoChildren(newBranch);

			SearchQueueBatch batch = createBatch();
			// 5. reverse folders in new branch
			subSubFolder.moveTo(mockActionContext(), folder, batch);
			subFolder.moveTo(mockActionContext(), subSubFolder, batch);

			// 6. assert for new branch
			assertThat(folder).as("folder").hasChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasNoChildren(newBranch);

			// 7. assert for initial branch
			assertThat(folder).as("folder").hasChildren(initialBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasChildren(initialBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasNoChildren(initialBranch);

			// 8. delete folder for initial release
			BulkActionContext context = createBulkContext();
			InternalActionContext ac = mockActionContext("");
			ac.getDeleteParameters().setRecursive(true);
			subFolder.deleteFromBranch(ac, initialBranch, context, false);

			// 9. assert for new branch
			assertThat(folder).as("folder").hasChildren(newBranch, subSubFolder);
			assertThat(subSubFolder).as("subSubFolder").hasChildren(newBranch, subFolder);
			assertThat(subFolder).as("subFolder").hasNoChildren(newBranch);

			// 10. assert for initial branch
			List<Node> nodes = new ArrayList<>();
			project.getNodeRoot().findAll(mockActionContext("release=" + initialBranch.getName()), new PagingParametersImpl(1, 10000L, "name",
				SortOrder.ASCENDING)).forEach(node -> nodes.add(node));
			assertThat(nodes).as("Nodes in initial branch").usingElementComparatorOnFields("uuid").doesNotContain(subFolder, subSubFolder);
			assertThat(folder).as("folder").hasNoChildren(initialBranch);

			// 11. assert searchqueuebatch
			Map<String, ElementEntry> affectedElements = new HashMap<>();
			affectedElements.put("subFolder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, subFolderUuid, project.getUuid(), initialBranch
				.getUuid(), ContainerType.DRAFT, "en"));
			affectedElements.put("subSubFolder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, subSubFolderUuid, project.getUuid(),
				initialBranch.getUuid(), ContainerType.DRAFT, "en"));
			assertThat(context.batch()).containsEntries(affectedElements);
		}
	}

	@Test
	public void testDeletePublished() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			SchemaContainerVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder and publish
			String folderUuid = db().tx(() -> {
				Node folder = project.getBaseNode().create(user(), folderSchema, project);
				BulkActionContext bac = createBulkContext();
				folder.applyPermissions(bac.batch(), role(), false, new HashSet<>(Arrays.asList(GraphPermission.READ_PERM,
					GraphPermission.READ_PUBLISHED_PERM)), Collections.emptySet());
				folder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("Folder");
				folder.publish(mockActionContext(), bac);
				return folder.getUuid();
			});

			// 2. assert published and draft node
			db().tx(() -> {
				List<String> nodeUuids = new ArrayList<>();
				project.getNodeRoot().findAll(mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").contains(folderUuid);
				nodeUuids.clear();
				project.getNodeRoot().findAll(mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").contains(folderUuid);
				return null;
			});

			// 3. delete
			InternalActionContext ac = mockActionContext("");
			BulkActionContext context = db().tx(() -> {
				BulkActionContext innerContext = createBulkContext();
				meshRoot().getNodeRoot().findByUuid(folderUuid).deleteFromBranch(ac, initialBranch, innerContext, false);
				return innerContext;
			});

			// 4. assert published and draft gone
			db().tx(() -> {
				List<String> nodeUuids = new ArrayList<>();
				project.getNodeRoot().findAll(mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").doesNotContain(folderUuid);

				nodeUuids.clear();
				project.getNodeRoot().findAll(mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, SortOrder.UNSORTED))
					.forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").doesNotContain(folderUuid);
				return null;
			});

			// 5. assert searchqueuebatch
			db().tx(() -> {
				Map<String, ElementEntry> affectedElements = new HashMap<>();
				affectedElements.put("draft folder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, folderUuid, project.getUuid(),
					initialBranch.getUuid(), ContainerType.DRAFT, "en"));
				affectedElements.put("published folder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, folderUuid, project.getUuid(),
					initialBranch.getUuid(), ContainerType.PUBLISHED, "en"));

				assertThat(context.batch()).containsEntries(affectedElements);
				return null;
			});
		}
	}

	@Test
	public void testDeletePublishedFromBranch() {
		try (Tx tx = tx()) {
			Project project = project();
			Branch initialBranch = project.getInitialBranch();
			SchemaContainerVersion folderSchema = schemaContainer("folder").getLatestVersion();

			// 1. create folder and publish
			String folderUuid = tx(() -> {
				Node folder = project.getBaseNode().create(user(), folderSchema, project);
				BulkActionContext bac = createBulkContext();
				folder.applyPermissions(bac.batch(), role(), false, new HashSet<>(Arrays.asList(GraphPermission.READ_PERM,
					GraphPermission.READ_PUBLISHED_PERM)), Collections.emptySet());
				folder.createGraphFieldContainer(english(), initialBranch, user()).createString("name").setString("Folder");
				folder.publish(mockActionContext(), bac);
				return folder.getUuid();
			});

			// 2. create new branch and migrate nodes
			tx(() -> {
				Branch newBranch = project.getBranchRoot().create("newbranch", user());
				meshDagger().branchMigrationHandler().migrateBranch(newBranch, null).blockingAwait();
				System.out.println("Branch UUID: " + newBranch.getUuid());
			});

			// 3. delete from initial branch
			InternalActionContext ac = mockActionContext("");
			BulkActionContext context = createBulkContext();
			tx(() -> {
				meshRoot().getNodeRoot().findByUuid(folderUuid).deleteFromBranch(ac, initialBranch, context, false);
			});

			// 4. assert published and draft gone from initial branch
			tx(() -> {
				List<String> nodeUuids = new ArrayList<>();
				project.getNodeRoot().findAll(mockActionContext("version=draft&branch=" + initialBranch.getUuid()), new PagingParametersImpl(1,
					10000L, null, UNSORTED)).forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").doesNotContain(folderUuid);

				nodeUuids.clear();
				project.getNodeRoot().findAll(mockActionContext("version=published&branch=" + initialBranch.getUuid()), new PagingParametersImpl(1,
					10000L, null, UNSORTED)).forEach(node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").doesNotContain(folderUuid);
			});

			// 5. assert published and draft still there for new branch
			tx(() -> {
				List<String> nodeUuids = new ArrayList<>();
				project.getNodeRoot().findAll(mockActionContext("version=draft"), new PagingParametersImpl(1, 10000L, null, UNSORTED)).forEach(
					node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Draft nodes").contains(folderUuid);

				nodeUuids.clear();
				project.getNodeRoot().findAll(mockActionContext("version=published"), new PagingParametersImpl(1, 10000L, null, UNSORTED)).forEach(
					node -> nodeUuids.add(node.getUuid()));
				assertThat(nodeUuids).as("Published nodes").contains(folderUuid);
			});

			// 6. assert searchqueuebatch
			tx(() -> {
				Map<String, ElementEntry> expectedEntries = new HashMap<>();
				expectedEntries.put("draft folder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, folderUuid, project.getUuid(),
					initialBranch.getUuid(), ContainerType.DRAFT, "en"));
				expectedEntries.put("published folder", new ElementEntry(SearchQueueEntryAction.DELETE_ACTION, folderUuid, project.getUuid(),
					initialBranch.getUuid(), ContainerType.PUBLISHED, "en"));
				assertThat(context.batch()).containsEntries(expectedEntries);
			});
		}
	}
}
