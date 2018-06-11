package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeDeleteEndpointTest extends AbstractMeshTest {

	/**
	 * Test deleting the last remaining language from a node. The node is also a container which has additional child elements. Assert that the node and all
	 * subnodes are deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteLastLanguageFromNode() throws Exception {
		Node node = folder("news");
		String branchName = "newBranch";

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		call(() -> client().createBranch(PROJECT_NAME, new BranchCreateRequest().setName(branchName)));
		failingLatch(latch);

		Set<String> childrenUuids = new HashSet<>();
		try (Tx tx = tx()) {
			assertThat(node.getChildren(initialBranchUuid())).as("The node must have children").isNotEmpty();
			for (Node child : node.getChildren(initialBranchUuid())) {
				collectUuids(child, childrenUuids);
			}
		}
		System.out.println("Collected: " + childrenUuids.size());
		String uuid = tx(() -> node.getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));

		NodeResponse response = call(
				() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranchUuid())));
		assertThat(response.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Also verify that the node is loadable in the other branch
		NodeResponse responseForBranch = call(
				() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch("newBranch")));
		assertThat(responseForBranch.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Delete first language container: german
		// The node should still be loadable and all child elements should still be existing
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "de"));
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		assertThat(response.getAvailableLanguages()).as("The node should only have a single container/language").hasSize(1);
		assertThatSubNodesExist(childrenUuids,initialBranchUuid());
		assertThatSubNodesExist(childrenUuids,branchName);

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
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "en", new DeleteParametersImpl().setRecursive(true)));
		// Verify that the node is still loadable in the initial branch
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(initialBranchUuid())));
		
		// TODO BUG - Issue #119
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(branchName)));
		assertNull("We currently expect the node to be returned but without any contents.",nodeResponse.getLanguage());
		//call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setBranch(branchName)), NOT_FOUND,
		//		"object_not_found_for_uuid", uuid);
		assertThatSubNodesWereDeleted(childrenUuids, branchName);
		assertThatSubNodesExist(childrenUuids, initialBranchUuid());
	}

	private void assertThatSubNodesWereDeleted(Set<String> childrenUuids, String branchName) {
		// Verify that all sub nodes have been deleted as well.
		for (String childUuid : childrenUuids) {
			System.out.println("Checking child: " + childUuid);
			NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setBranch(branchName)));
			assertNull("We currently expect the node to be returned but without any contents.",nodeResponse.getLanguage());
			// TODO BUG - Issue #119
			// call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setBranch(branchName)), NOT_FOUND, "object_not_found_for_uuid", childUuid);
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
