package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_BRANCH_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
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
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeDeleteEndpointTest extends AbstractMeshTest {

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
		assertTrue(responseA.getFields().isEmpty());

		NodeListResponse childrenA = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setBranch(INITIAL_BRANCH_NAME)));
		Optional<NodeResponse> childA = childrenA.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertFalse("The node was deleted from the branch and should not be present.", childA.isPresent());

		NodeListResponse childrenB = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setBranch(SECOND_BRANCH_NAME)));
		Optional<NodeResponse> childB = childrenB.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertTrue(childB.isPresent());

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
