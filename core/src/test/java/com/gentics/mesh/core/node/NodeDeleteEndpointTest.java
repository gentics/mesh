package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.INITIAL_RELEASE_NAME;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.VersioningParameters;
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
		String releaseName = "newRelease";

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		call(() -> client().createRelease(PROJECT_NAME, new ReleaseCreateRequest().setName(releaseName)));
		failingLatch(latch);

		Set<String> childrenUuids = new HashSet<>();
		try (Tx tx = tx()) {
			assertThat(node.getChildren(initialReleaseUuid())).as("The node must have children").isNotEmpty();
			for (Node child : node.getChildren(initialReleaseUuid())) {
				collectUuids(child, childrenUuids);
			}
		}
		System.out.println("Collected: " + childrenUuids.size());
		String uuid = tx(() -> node.getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid, new PublishParametersImpl().setRecursive(true)));

		NodeResponse response = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(initialReleaseUuid())));
		assertThat(response.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Also verify that the node is loadable in the other release
		NodeResponse responseForRelease = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease("newRelease")));
		assertThat(responseForRelease.getAvailableLanguages()).as("The node should have two container").hasSize(2);

		// Delete first language container: german
		// The node should still be loadable and all child elements should still be existing
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "de"));
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		assertThat(response.getAvailableLanguages()).as("The node should only have a single container/language").hasSize(1);
		assertThatSubNodesExist(childrenUuids, initialReleaseUuid());
		assertThatSubNodesExist(childrenUuids, releaseName);

		// Delete the second language container (english) - The delete should fail since this would be the last language for this node
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "en"), BAD_REQUEST, "node_error_delete_failed_last_container_for_release");
		assertThatSubNodesExist(childrenUuids, releaseName);
		assertThatSubNodesExist(childrenUuids, initialReleaseUuid());

		// Delete the whole node without the recursive flag - This should fail due to the child element check
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new DeleteParametersImpl().setRecursive(false)), BAD_REQUEST,
			"node_error_delete_failed_node_has_children");
		assertThatSubNodesExist(childrenUuids, releaseName);
		assertThatSubNodesExist(childrenUuids, initialReleaseUuid());

		// Delete the second language container (english) and use the recursive flag. The node and all subnodes should have been removed in the current release
		call(() -> client().deleteNode(PROJECT_NAME, uuid, "en", new DeleteParametersImpl().setRecursive(true)));
		// Verify that the node is still loadable in the initial release
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(initialReleaseUuid())));

		// TODO BUG - Issue #119
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(releaseName)));
		assertNull("We currently expect the node to be returned but without any contents.", nodeResponse.getLanguage());
		// call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(releaseName)), NOT_FOUND,
		// "object_not_found_for_uuid", uuid);
		assertThatSubNodesWereDeleted(childrenUuids, releaseName);
		assertThatSubNodesExist(childrenUuids, initialReleaseUuid());
	}

	@Test
	public void testDeleteNodeFromRelease() {
		grantAdminRole();
		final String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		final String parentNodeUuid = tx(() -> folder("news").getUuid());
		final String SECOND_BRANCH_NAME = "branch2";

		// 1. Create node in release a
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
		updateNode(1, uuid, INITIAL_RELEASE_NAME);
		updateNode(2, uuid, INITIAL_RELEASE_NAME);

		// 3. Publish node
		call(() -> client().publishNode(PROJECT_NAME, uuid));

		// 4. Create two more drafts
		updateNode(3, uuid, INITIAL_RELEASE_NAME);
		updateNode(4, uuid, INITIAL_RELEASE_NAME);

		// 5. Create new release
		waitForJobs(() -> {
			ReleaseCreateRequest releaseCreateRequest = new ReleaseCreateRequest();
			releaseCreateRequest.setName(SECOND_BRANCH_NAME);
			call(() -> client().createRelease(PROJECT_NAME, releaseCreateRequest));
		}, MigrationStatus.COMPLETED, 1);

		// Create two drafts in release A
		updateNode(5, uuid, INITIAL_RELEASE_NAME);
		updateNode(6, uuid, INITIAL_RELEASE_NAME);

		// Create two drafts in release B
		updateNode(5, uuid, SECOND_BRANCH_NAME);
		updateNode(6, uuid, SECOND_BRANCH_NAME);

		// Delete node in release A
		call(() -> client().deleteNode(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(INITIAL_RELEASE_NAME)));

		// Check that the node is still alive in the other release
		NodeResponse responseB = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(SECOND_BRANCH_NAME)));
		assertEquals("en", responseB.getLanguage());
		assertFalse(responseB.getFields().isEmpty());

		// And deleted in the first release
		NodeResponse responseA = call(
			() -> client().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParametersImpl().setRelease(INITIAL_RELEASE_NAME)));
		assertNull(responseA.getLanguage());
		assertTrue(responseA.getFields().isEmpty());

		NodeListResponse childrenA = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setRelease(INITIAL_RELEASE_NAME)));
		Optional<NodeResponse> childA = childrenA.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertFalse("The node was deleted from the release and should not be present.", childA.isPresent());

		NodeListResponse childrenB = call(
			() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().setRelease(SECOND_BRANCH_NAME)));
		Optional<NodeResponse> childB = childrenB.getData().stream().filter(n -> n.getUuid().equals(uuid)).findFirst();
		assertTrue(childB.isPresent());

	}

	private void updateNode(int i, String uuid, String release) {
		VersioningParameters param = new VersioningParametersImpl().setRelease(release);
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, param));
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
		nodeUpdateRequest.setVersion(response.getVersion());
		nodeUpdateRequest.setLanguage("en");
		call(() -> client().updateNode(PROJECT_NAME, response.getUuid(), nodeUpdateRequest, param));
	}

	private void assertThatSubNodesWereDeleted(Set<String> childrenUuids, String releaseName) {
		// Verify that all sub nodes have been deleted as well.
		for (String childUuid : childrenUuids) {
			System.out.println("Checking child: " + childUuid);
			NodeResponse nodeResponse = call(
				() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setRelease(releaseName)));
			assertNull("We currently expect the node to be returned but without any contents.", nodeResponse.getLanguage());
			// TODO BUG - Issue #119
			// call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setRelease(releaseName)), NOT_FOUND,
			// "object_not_found_for_uuid", childUuid);
		}
	}

	private void assertThatSubNodesExist(Set<String> uuids, String releaseName) {
		for (String childUuid : uuids) {
			System.out.println("Checking child: " + childUuid);
			call(() -> client().findNodeByUuid(PROJECT_NAME, childUuid, new VersioningParametersImpl().setRelease(releaseName)));
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
		for (Node subchild : child.getChildren(initialReleaseUuid())) {
			collectUuids(subchild, childrenUuids);
		}
	}
}
