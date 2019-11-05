package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.BranchMigrationContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeDeleteEndpointTest extends AbstractMeshTest {

	private Random rnd = new Random();

	/**
	 * Test deleting the last remaining language from a node. The node is also a container which has additional child elements. Assert that the node and all
	 * subnodes are deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteLastLanguageFromNode() throws Exception {
		grantAdminRole();
		Node node = folder("news");
		String branchName = "newBranch";

		waitForLatestJob(() -> {
			call(() -> client().createBranch(PROJECT_NAME, new BranchCreateRequest().setName(branchName)));
		});

		String branchUuid = call(() -> client().findBranches(PROJECT_NAME)).getData().stream().filter(b -> b.getName().equals(branchName)).findFirst()
			.get().getUuid();

		Set<String> childrenUuids = new HashSet<>();
		try (Tx tx = tx()) {
			assertThat(node.getChildren(initialBranchUuid())).as("The node must have children").isNotEmpty();
			for (Node child : node.getChildren(initialBranchUuid())) {
				collectUuids(child, childrenUuids);
			}
		}
		String uuid = tx(() -> node.getUuid());
		String schemaUuid = tx(() -> node.getSchemaContainer().getUuid());
		String schemaName = tx(() -> node.getSchemaContainer().getName());
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));

		NodeResponse response = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranchUuid())));
		assertThat(response.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Also verify that the node is loadable in the other branch
		NodeResponse responseForRelease = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch("newBranch")));
		assertThat(responseForRelease.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Delete first language container: german
		expect(NODE_CONTENT_DELETED).match(1, NodeMeshEventModel.class, event -> {
			assertEquals("de", event.getLanguageTag());
			assertEquals(branchUuid, event.getBranchUuid());
			assertEquals(uuid, event.getUuid());

			SchemaReference schemaRef = event.getSchema();
			assertNotNull(schemaRef);
			assertEquals(schemaName, schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());
			assertNotNull(schemaRef.getVersion());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());
		});
		// The node should still be loadable and all child elements should still be existing
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "de"));
		awaitEvents();
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		assertThat(response.getAvailableLanguages()).as("The node should only have a single container/language").hasSize(1);
		assertThatSubNodesExist(childrenUuids, initialBranchUuid());
		assertThatSubNodesExist(childrenUuids, branchName);

		// Delete the second language container (english) - The delete should fail since this would be the last language for this node
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "en"), BAD_REQUEST, "node_error_delete_failed_last_container_for_branch");
		assertThatSubNodesExist(childrenUuids, branchName);
		assertThatSubNodesExist(childrenUuids, initialBranchUuid());

		// Delete the whole node without the recursive flag - This should fail due to the child element check
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(false)), BAD_REQUEST,
			"node_error_delete_failed_node_has_children");
		assertThatSubNodesExist(childrenUuids, branchName);
		assertThatSubNodesExist(childrenUuids, initialBranchUuid());

		// Delete the second language container (english) and use the recursive flag. The node and all subnodes should have been removed in the current branch
		expect(NODE_CONTENT_DELETED).match(1, NodeMeshEventModel.class, event -> {
			assertEquals("en", event.getLanguageTag());
			assertEquals(branchUuid, event.getBranchUuid());
			assertEquals(uuid, event.getUuid());

			SchemaReference schemaRef = event.getSchema();
			assertNotNull(schemaRef);
			assertEquals(schemaName, schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());
			assertNotNull(schemaRef.getVersion());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());
		});
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "en", new DeleteParametersImpl().setRecursive(true)));
		awaitEvents();
		// Verify that the node is still loadable in the initial branch
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranchUuid())));

		// TODO BUG - Issue #119
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(branchName)));
		assertNull("We currently expect the node to be returned but without any contents.", nodeResponse.getLanguage());
		// call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(branchName)), NOT_FOUND,
		// "object_not_found_for_uuid", uuid);
		assertThatSubNodesWereDeleted(childrenUuids, branchName);
		assertThatSubNodesExist(childrenUuids, initialBranchUuid());
	}

	@Test
	public void testDeleteNodeFromRelease() {
		grantAdminRole();
		final String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		final String parentNodeUuid = tx(() -> folder("news").getUuid());
		final String SECOND_BRANCH_NAME = "branch2";

		// 1. Create node in branch a
		NodeCreateRequest request = new NodeCreateRequest();
		SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
		schemaReference.setName("content");
		schemaReference.setUuid(schemaUuid);
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setSchema(schemaReference);
		request.setLanguage("en");
		request.setParentNodeUuid(parentNodeUuid);
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		String uuid = response.getUuid();

		// 2. Create two drafts
		updateNode(1, uuid, INITIAL_BRANCH_NAME);
		updateNode(2, uuid, INITIAL_BRANCH_NAME);

		// 3. Publish node
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// 4. Create two more drafts
		updateNode(3, uuid, INITIAL_BRANCH_NAME);
		updateNode(4, uuid, INITIAL_BRANCH_NAME);

		// 5. Create new branch
		waitForJobs(() -> {
			BranchCreateRequest branchCreateRequest = new BranchCreateRequest();
			branchCreateRequest.setName(SECOND_BRANCH_NAME);
			call(() -> client().createBranch(PROJECT_NAME, branchCreateRequest));
		}, JobStatus.COMPLETED, 1);

		// Create two drafts in branch A
		updateNode(5, uuid, INITIAL_BRANCH_NAME);
		updateNode(6, uuid, INITIAL_BRANCH_NAME);

		// Create two drafts in branch B
		updateNode(5, uuid, SECOND_BRANCH_NAME);
		updateNode(6, uuid, SECOND_BRANCH_NAME);

		// Delete node in branch A
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(INITIAL_BRANCH_NAME)));

		// Check that the node is still alive in the other branch
		NodeResponse responseB = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(SECOND_BRANCH_NAME)));
		assertEquals("en", responseB.getLanguage());
		assertFalse(responseB.getFields().isEmpty());

		// And deleted in the first branch
		NodeResponse responseA = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(INITIAL_BRANCH_NAME)));
		assertNull(responseA.getLanguage());
		assertThat(responseA.getFields()).isNull();

		NodeListResponse childrenA = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setBranch(INITIAL_BRANCH_NAME)));
		Optional<NodeResponse> childA = childrenA.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertFalse("The node was deleted from the branch and should not be present.", childA.isPresent());

		NodeListResponse childrenB = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setBranch(SECOND_BRANCH_NAME)));
		Optional<NodeResponse> childB = childrenB.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertTrue(childB.isPresent());

	}

	@Test
	public void testDeleteForBranch() throws Exception {
		Node node = content("concorde");
		String uuid = tx(() -> node.getUuid());

		// Create new branch
		Branch newBranch = tx(() -> createBranch("newbranch"));

		BranchMigrationContextImpl context = new BranchMigrationContextImpl();
		context.setNewBranch(newBranch);
		context.setOldBranch(tx(() -> initialBranch()));
		meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();

		String newBranchUuid = tx(() -> newBranch.getUuid());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setBranch(newBranchUuid)));

		// Delete node in new branch
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(newBranch.getUuid())));

		// Assert that the node was only deleted in the new branch
		try (Tx tx = tx()) {
			assertElement(project().getNodeRoot(), uuid, true);
			assertThat(node.getGraphFieldContainers(initialBranch(), DRAFT)).as("draft containers for initial branch").isNotEmpty();
			assertThat(node.getGraphFieldContainers(newBranch, DRAFT)).as("draft containers for new branch").isEmpty();
		}

	}

	@Test
	@Ignore
	public void testDeletePublishedForBranch() throws Exception {
		Node node = content("concorde");
		String uuid = tx(() -> node.getUuid());

		Branch newBranch = tx(() -> {
			// Publish the node
			BulkActionContext bac = createBulkContext();
			node.publish(mockActionContext(), bac);

			// Create new branch
			Branch b = createBranch("newbranch");

			// Migrate nodes
			BranchMigrationContextImpl context = new BranchMigrationContextImpl();
			context.setNewBranch(b);
			context.setOldBranch(initialBranch());
			meshDagger().branchMigrationHandler().migrateBranch(context).blockingAwait();
			return b;
		});

		String newBranchUuid = tx(() -> newBranch.getUuid());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().draft().setBranch(newBranchUuid)));

		// Delete node in new branch
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(newBranchUuid)));

		// Assert deletion - nodes should only be deleted for new branch
		try (Tx tx = tx()) {
			assertElement(project().getNodeRoot(), uuid, true);
			assertThat(node.getGraphFieldContainers(initialBranch(), ContainerType.DRAFT)).as("draft containers for initial branch").isNotEmpty();
			assertThat(node.getGraphFieldContainers(initialBranch(), ContainerType.PUBLISHED)).as("published containers for initial branch")
				.isNotEmpty();
			assertThat(node.getGraphFieldContainers(newBranch, ContainerType.DRAFT)).as("draft containers for new branch").isEmpty();
			assertThat(node.getGraphFieldContainers(newBranch, ContainerType.PUBLISHED)).as("published containers for new branch").isEmpty();
		}
	}

	@Test
	public void testDeleteBaseNode() throws Exception {
		try (Tx tx = tx()) {
			Node node = project().getBaseNode();
			String uuid = node.getUuid();

			call(() -> client().deleteNode(PROJECT_NAME, uuid), METHOD_NOT_ALLOWED, "node_basenode_not_deletable");

			Node foundNode = project().getNodeRoot().findByUuid(uuid);
			assertNotNull("The node should still exist.", foundNode);
		}
	}

	@Test
	@Ignore
	public void testDeleteRecursiveFromBranch2() throws InterruptedException {
		grantAdminRole();
		String newsFolderUuid = tx(() -> folder("news").getUuid());

		List<String> uuids = new ArrayList<>();
		createTree(uuids, newsFolderUuid, 90, 0, initialBranchUuid(), true);
		String newBranchUuid = createBranch();

		// Now take everything in the new branch offline
		System.out.println("Taking nodes in new branch offline");
		call(() -> client().takeNodeOffline(projectName(), newsFolderUuid, new PublishParametersImpl().setRecursive(true),
			new VersioningParametersImpl().setBranch(newBranchUuid)));
		System.out.println("Done");

		CountDownLatch allDone = new CountDownLatch(1 + 1);
		// Invoke publish
		publish(allDone, newsFolderUuid, newBranchUuid);

		// Invoke delete
		deleteNode(allDone, newsFolderUuid, initialBranchUuid());

		// 2. Create node
		// for (int i = 0; i < 5; i++) {
		// createNode(allDone, uuids, initialBranchUuid());
		// }

		allDone.await(30, TimeUnit.SECONDS);
		System.out.println("All done. Testing delete again..");
		deleteParent(newsFolderUuid, initialBranchUuid());
		System.out.println("Done!");

	}

	private void publish(CountDownLatch allDone, String uuid, String branchUuid) {
		vertx().executeBlocking(bc -> {
			try {
				System.out.println("Publishing node {" + uuid + "}");
				call(() -> client().publishNode(projectName(), uuid, new PublishParametersImpl().setRecursive(true),
					new VersioningParametersImpl().setBranch(branchUuid)));
				bc.complete();
			} catch (Throwable t) {
				bc.fail(t);
			}
		}, false, rh -> {
			if (rh.failed()) {
				System.out.println("Publish failed");
				rh.cause().printStackTrace();
			} else {
				System.out.println("Publish done");
			}
			allDone.countDown();
		});

	}

	private void createNode(CountDownLatch allDone, List<String> uuids, String branchUuid) {
		vertx().executeBlocking(bc -> {
			try {
				String parentNodeUuid = random(uuids);
				System.out.println("Creating node in {" + parentNodeUuid + "}");
				createNode(null, parentNodeUuid, "i:" + System.currentTimeMillis(), branchUuid, true);
				bc.complete();
			} catch (Throwable t) {
				bc.fail(t);
			}
		}, false, rh -> {
			if (rh.failed()) {
				System.out.println("Create failed");
				rh.cause().printStackTrace();
			} else {
				System.out.println("Create done");
			}
			allDone.countDown();
		});
	}

	private void deleteNode(CountDownLatch allDone, String uuid, String branchUuid) {
		vertx().executeBlocking(bc -> {
			System.out.println("Deleting node");
			try {
				deleteParent(uuid, branchUuid);
				bc.complete();
			} catch (Throwable t) {
				bc.fail(t);
			}
		}, false, rh -> {
			if (rh.failed()) {
				System.out.println("Delete failed");
				rh.cause().printStackTrace();
			} else {
				System.out.println("Delete done");
			}
			allDone.countDown();
		});
	}

	private void deleteParent(String uuid, String branchUuid) {
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(branchUuid),
			new DeleteParametersImpl().setRecursive(true)));

	}

	private String random(List<String> uuids) {
		int idx = rnd.nextInt(uuids.size());
		System.out.println("Idx: " + idx);
		return uuids.get(idx);
	}

	@Test
	public void testDeleteRecursiveFromBranch() {
		grantAdminRole();
		String newsFolderUuid = tx(() -> folder("news").getUuid());
		String branchedNodeUuid = tx(() -> content("concorde").getUuid());
		String nodeInBoth = createNode(null, newsFolderUuid, "root", initialBranchUuid(), false).getUuid();
		// call(() -> client().publishNode(projectName(), baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
		String branchUuid = createBranch();

		final int nNodes = 10;
		// NodeResponse folder1 = createFolder(nodeUuid, 1, initialBranchUuid());
		Map<Integer, String> uuids = new HashMap<>();
		for (int i = 0; i <= nNodes; i++) {
			// Create node in new branch
			NodeResponse response = createNode(null, newsFolderUuid, "" + i, initialBranchUuid(), false);
			uuids.put(i, response.getUuid());
			if (i % 10 == 0) {
				System.out.println("Creating node {" + i + "} of {" + nNodes + "}");
			}
		}
		// Create nodes in branch using initial branch node uuids
		// NodeResponse folder2 = createFolder(nodeUuid, 1, branchUuid.get());
		for (int i = 0; i <= nNodes; i++) {
			// Create node in new branch
			String uuid = uuids.get(i);
			createNode(uuid, newsFolderUuid, "" + i, branchUuid, true);
			if (i % 10 == 0) {
				System.out.println("Creating node {" + i + "} of {" + nNodes + "}");
			}
		}

		// Create nodes in new branch
		for (int i = 0; i <= nNodes; i++) {
			// Create node in new branch
			createNode(null, newsFolderUuid, "b" + i, branchUuid, true);
			if (i % 10 == 0) {
				System.out.println("Creating node {" + i + "} of {" + nNodes + "}");
			}
		}

		// Create nodes only in initial branch
		for (int i = 0; i <= nNodes; i++) {
			// Create node in new branch
			createNode(null, newsFolderUuid, "c" + i, initialBranchUuid(), false);
			if (i % 10 == 0) {
				System.out.println("Creating node {" + i + "} of {" + nNodes + "}");
			}
		}

		call(() -> client().publishNode(projectName(), nodeInBoth, new VersioningParametersImpl().setBranch(branchUuid)));
		call(() -> client().takeNodeOffline(projectName(), nodeInBoth, new VersioningParametersImpl().setBranch(initialBranchUuid())));

		// NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		// nodeUpdateRequest.setLanguage("de");
		// FieldMap updateFields = new FieldMapImpl();
		// updateFields.put("teaser", FieldUtil.createStringField("ein teaser"));
		// updateFields.put("slug", FieldUtil.createStringField("neue-seite.html"));
		// updateFields.put("content", FieldUtil.createStringField("Mahlzeit!"));
		// nodeUpdateRequest.setFields(updateFields);
		// call(() -> client().updateNode(projectName(), nodeResponse.getUuid(), nodeUpdateRequest, branchParam));

		// Delete a node in the initial branch.
		call(() -> client().deleteNode(projectName(), branchedNodeUuid, new VersioningParametersImpl().setBranch(branchUuid)));

		// Verify node can be loaded
		call(() -> client().findNodeByUuid(PROJECT_NAME, newsFolderUuid, new VersioningParametersImpl().draft().setBranch(initialBranchUuid())));
		call(() -> client().findNodeByUuid(PROJECT_NAME, newsFolderUuid, new VersioningParametersImpl().draft().setBranch(branchUuid)));
		call(() -> client().findNodeByUuid(PROJECT_NAME, branchedNodeUuid, new VersioningParametersImpl().draft().setBranch(branchUuid)));

		// Delete node in new branch
		call(() -> client().deleteNode(PROJECT_NAME, newsFolderUuid, new VersioningParametersImpl().setBranch(initialBranchUuid()),
			new DeleteParametersImpl().setRecursive(true)));

		call(() -> client().deleteNode(PROJECT_NAME, newsFolderUuid, new VersioningParametersImpl().setBranch(branchUuid),
			new DeleteParametersImpl().setRecursive(true)));

	}

	private void createTree(List<String> uuids, String parentUuid, int depth, int pages, String branchUuid, boolean publish) {
		if (depth <= 0) {
			System.out.println("Done");
			return;
		} else {
			NodeResponse node = createFolder(parentUuid, depth, branchUuid, publish);
			System.out.println(depth + ": " + parentUuid + " -> " + node.getUuid());
			uuids.add(node.getUuid());
			for (int i = 0; i < pages; i++) {
				NodeResponse content = createNode(null, node.getUuid(), "p" + i, branchUuid, publish);
				System.out.println("    --> " + content.getUuid());
			}
			createTree(uuids, node.getUuid(), --depth, pages, branchUuid, publish);
		}
	}

	private NodeResponse createFolder(String parentNodeUuid, int i, String branchUuid, boolean publish) {
		VersioningParameters branchParam = new VersioningParametersImpl().setBranch(branchUuid);
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		FieldMap fields = new FieldMapImpl();
		fields.put("teaser", FieldUtil.createStringField("some teaser" + i));
		fields.put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
		fields.put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.setFields(fields);
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		NodeResponse nodeResponse = call(
			() -> client().createNode(projectName(), nodeCreateRequest, branchParam));
		if (publish) {
			call(() -> client().publishNode(projectName(), nodeResponse.getUuid(), branchParam));
		}
		return nodeResponse;
	}

	private String createBranch() {
		AtomicReference<String> branchUuid = new AtomicReference<>();
		waitForJob(() -> {
			BranchCreateRequest request = new BranchCreateRequest().setName("newBranch").setLatest(false);
			String uuid = call(() -> client().createBranch(projectName(), request)).getUuid();
			branchUuid.set(uuid);
		});
		return branchUuid.get();
	}

	private NodeResponse createNode(String uuid, String parentNodeUuid, String postfix, String branchUuid, boolean publish) {
		VersioningParameters branchParam = new VersioningParametersImpl().setBranch(branchUuid);
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		FieldMap fields = new FieldMapImpl();
		fields.put("name", FieldUtil.createStringField("Folder " + postfix));
		fields.put("slug", FieldUtil.createStringField("folder" + postfix));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setFields(fields);
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		NodeResponse nodeResponse;
		if (uuid == null) {
			nodeResponse = call(
				() -> client().createNode(projectName(), nodeCreateRequest, branchParam));
		} else {
			nodeResponse = call(
				() -> client().createNode(uuid, projectName(), nodeCreateRequest, branchParam));
		}
		if (publish) {
			call(() -> client().publishNode(projectName(), nodeResponse.getUuid(), branchParam));
		}
		return nodeResponse;
	}

	private void updateNode(int i, String uuid, String branch) {
		VersioningParameters param = new VersioningParametersImpl().setBranch(branch);
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, param));
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
		nodeUpdateRequest.setVersion(response.getVersion());
		nodeUpdateRequest.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest, param));
	}

	private void assertThatSubNodesWereDeleted(Set<String> childrenUuids, String branchName) {
		// Verify that all sub nodes have been deleted as well.
		for (String childUuid : childrenUuids) {
			System.out.println("Checking child: " + childUuid);
			NodeResponse nodeResponse = call(
				() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setBranch(branchName)));
			assertNull("We currently expect the node to be returned but without any contents.", nodeResponse.getLanguage());
			// TODO BUG - Issue #119
			// call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setBranch(branchName)), NOT_FOUND,
			// "object_not_found_for_uuid", childUuid);
		}
	}

	private void assertThatSubNodesExist(Set<String> uuids, String branchName) {
		for (String childUuid : uuids) {
			System.out.println("Checking child: " + childUuid);
			call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setBranch(branchName)));
		}
	}

	/**
	 * Recursively collect all uuid of subnodes.
	 * 
	 * @param child
	 * @param childrenUuids
	 */
	private void collectUuids(Node child, Set<String> childrenUuids) {
		childrenUuids.add(child.getUuid());
		for (Node subchild : child.getChildren(initialBranchUuid())) {
			collectUuids(subchild, childrenUuids);
		}
	}
}
