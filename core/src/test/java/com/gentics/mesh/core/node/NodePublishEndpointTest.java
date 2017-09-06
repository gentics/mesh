package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Map.Entry;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodePublishEndpointTest extends AbstractMeshTest {

	/**
	 * Folder /news/2015 is not published. A new node will be created in folder 2015. Publishing the created folder should fail since the parent folder
	 * (/news/2015) is not yet published. This test will also assert that publishing works fine as soon as the parent node is published.
	 */
	@Test
	public void testPublishNodeInUnpublishedContainer() {

		// 1. Take the parent folder offline
		String parentFolderUuid;
		String subFolderUuid;
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext("recursive=true");
			Node subFolder = folder("2015");
			Node parentFolder = folder("news");
			SearchQueueBatch batch = createBatch();
			parentFolder.publish(ac, batch);
			subFolder.takeOffline(ac, batch);
			subFolderUuid = subFolder.getUuid();
			parentFolderUuid = parentFolder.getUuid();
			tx.success();
		}

		assertPublishStatus("Node 2015 should not be published", subFolderUuid, false);
		assertPublishStatus("Node News should be published", parentFolderUuid, true);

		// 2. Create a new node in the folder 2015
		NodeCreateRequest requestA = new NodeCreateRequest();
		requestA.setLanguage("en");
		requestA.setParentNodeUuid(subFolderUuid);
		requestA.setSchema(new SchemaReference().setName("content"));
		requestA.getFields().put("teaser", FieldUtil.createStringField("nodeA"));
		requestA.getFields().put("slug", FieldUtil.createStringField("nodeA"));
		NodeResponse nodeA = call(() -> client().createNode(PROJECT_NAME, requestA));

		// 3. Publish the created node - It should fail since the parentfolder is not published
		dummySearchProvider().clear();
		call(() -> client().publishNode(PROJECT_NAME, nodeA.getUuid()), BAD_REQUEST, "node_error_parent_containers_not_published", subFolderUuid);
		assertThat(dummySearchProvider()).hasEvents(0, 0, 0, 0, 0);

		// 4. Publish the parent folder
		call(() -> client().publishNode(PROJECT_NAME, subFolderUuid));

		// 5. Verify that publishing now works
		call(() -> client().publishNode(PROJECT_NAME, nodeA.getUuid()));

	}

	@Test
	public void testGetPublishStatusForEmptyLanguage() {
		try (Tx tx = tx()) {
			Node node = folder("products");
			call(() -> client().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "fr"), NOT_FOUND, "error_language_not_found", "fr");
		}
	}

	@Test
	public void testPublishDeleteCase() {

		String parentNodeUuid = tx(() -> folder("news").getUuid());

		NodeCreateRequest request = new NodeCreateRequest();
		request.setSchema(new SchemaReference().setName("content"));
		request.setLanguage("en");
		request.getFields().put("title", FieldUtil.createStringField("some title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.en.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setParentNodeUuid(parentNodeUuid);

		// Create a new node (en)
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		String nodeUuid = response.getUuid();
		call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "en"));

		// Create a new language for the node (de)
		NodeUpdateRequest updateRequest = new NodeUpdateRequest();
		updateRequest.getFields().put("title", FieldUtil.createStringField("some title"));
		updateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		updateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.en.html"));
		updateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		updateRequest.setLanguage("de");
		updateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.de.html"));
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, updateRequest));

		// Publish the language de
		call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"));

		// Delete the language de
		call(() -> client().deleteNode(PROJECT_NAME, nodeUuid, "de"));

		// Create a new language de
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, updateRequest));

		// Publish the language de
		call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"));
	}

	@Test
	public void testPublishNode() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			String releaseUuid = db().tx(() -> project().getLatestRelease().getUuid());
			String schemaContainerVersionUuid = db().tx(() -> node.getLatestDraftFieldContainer(english()).getSchemaContainerVersion().getUuid());

			PublishStatusResponse statusResponse = call(() -> client().publishNode(PROJECT_NAME, nodeUuid));
			assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");

			assertThat(dummySearchProvider()).hasStore(
					NodeGraphFieldContainer.composeIndexName(projectUuid(), releaseUuid, schemaContainerVersionUuid, PUBLISHED),
					NodeGraphFieldContainer.composeIndexType(), NodeGraphFieldContainer.composeDocumentId(nodeUuid, "en"));
			// The draft of the node must still remain in the index
			assertThat(dummySearchProvider()).hasEvents(1, 0, 0, 0, 0);

		}
	}

	@Test
	public void testGetPublishStatus() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();

			// 1. Check initial status
			PublishStatusResponse publishStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Initial publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");

			// 2. Take node offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));

			// 3. Assert that node is offline
			publishStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Publish status after take offline").isNotNull().isNotPublished("en").hasVersion("en", "1.0");

			// 4. Publish the node
			call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

			// 5. Assert that node has been published
			publishStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid));
			assertThat(publishStatus).as("Publish status after publish").isNotNull().isPublished("en").hasVersion("en", "2.0");
		}
	}

	@Test
	public void testGetPublishStatusForRelease() {
		Node node = folder("2015");
		Project project = project();
		Release newRelease;

		try (Tx tx = tx()) {
			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
			call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

			PublishStatusResponse publishStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid,
					new VersioningParametersImpl().setRelease(initialRelease().getName())));
			assertThat(publishStatus).as("Initial release publish status").isNotNull().isPublished("en").hasVersion("en", "1.0").doesNotContain("de");

			publishStatus = call(
					() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setRelease(newRelease.getName())));
			assertThat(publishStatus).as("New release publish status").isNotNull().isPublished("de").hasVersion("de", "1.0").doesNotContain("en");

			publishStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid, new NodeParametersImpl()));
			assertThat(publishStatus).as("New release publish status").isNotNull().isPublished("de").hasVersion("de", "1.0").doesNotContain("en");
		}
	}

	@Test
	public void testGetPublishStatusNoPermission() {
		Node node = folder("news");
		try (Tx tx = tx()) {
			role().revokePermissions(node, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testGetPublishStatusBogusUuid() {
		String bogusUuid = "bogus";
		call(() -> client().getNodePublishStatus(PROJECT_NAME, bogusUuid), NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	public void testGetPublishStatusForLanguage() {
		try (Tx tx = tx()) {
			Node node = folder("products");

			// 1. Take everything offline
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// 2. Publish only a specific language of a node
			call(() -> client().publishNodeLanguage(PROJECT_NAME, node.getUuid(), "en"));

			// 3. Assert that the other language is not published
			assertThat(call(() -> client().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "de"))).as("German publish status")
					.isNotPublished();
			assertThat(call(() -> client().getNodeLanguagePublishStatus(PROJECT_NAME, node.getUuid(), "en"))).as("English publish status")
					.isPublished();
		}
	}

	@Test
	public void testPublishNodeWithNoSegmentPathValue() {
		String uuid = db().tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid(uuid);
		request.setSchema(new SchemaReference().setName("content"));
		request.getFields().put("teaser", FieldUtil.createStringField("some-teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("some-slug"));
		request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
	}

	@Test
	public void testPublishNodeForRelease() {
		Project project = project();
		Node node = folder("2015");

		try (Tx tx = tx()) {
			project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("slug", FieldUtil.createStringField("2015"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

			// publish for the initial release
			PublishStatusResponse publishStatus = call(
					() -> client().publishNode(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setRelease(initialRelease().getName())));
			assertThat(publishStatus).as("Initial publish status").isPublished("en").hasVersion("en", "1.0").doesNotContain("de");
		}
	}

	@Test
	public void testPublishNodeNoPermission() {
		Node node = folder("2015");

		try (Tx tx = tx()) {
			role().revokePermissions(node, PUBLISH_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			call(() -> client().publishNode(PROJECT_NAME, nodeUuid), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testPublishNodeBogusUuid() {
		String bogusUuid = "bogus";
		call(() -> client().publishNode(PROJECT_NAME, bogusUuid), NOT_FOUND, "object_not_found_for_uuid", bogusUuid);
	}

	@Test
	public void testRepublishUnchanged() {
		String nodeUuid = db().tx(() -> folder("2015").getUuid());
		PublishStatusResponse statusResponse = call(() -> client().publishNode(PROJECT_NAME, nodeUuid));
		assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");

		statusResponse = call(() -> client().publishNode(PROJECT_NAME, nodeUuid));
		assertThat(statusResponse).as("Publish status").isNotNull().isPublished("en").hasVersion("en", "1.0");
	}

	/**
	 * Verify that the move action fails if the published node is moved into offline containers.
	 */
	@Test
	public void testMoveConsistency() {
		// 1. Take the target folder offline
		String newsFolderUuid = db().tx(() -> folder("news").getUuid());
		call(() -> client().takeNodeOffline(PROJECT_NAME, newsFolderUuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Move the published node into the offline target node
		String publishedNode = db().tx(() -> content("concorde").getUuid());
		call(() -> client().moveNode(PROJECT_NAME, publishedNode, newsFolderUuid), BAD_REQUEST, "node_error_parent_containers_not_published",
				newsFolderUuid);
	}

	@Test
	public void testPublishLanguage() {
		String nodeUuid = db().tx(() -> folder("2015").getUuid());
		String releaseUuid = db().tx(() -> latestRelease().getUuid());

		// Only publish the test node. Take all children offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

		// Update german language -> new draft
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("de");
		update.getFields().put("name", FieldUtil.createStringField("changed-de"));
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

		// assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParametersImpl().setLanguages("de"),
		// new VersioningParametersImpl().published())).getAvailableLanguages()).containsOnly("en");

		call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().published()), NOT_FOUND, "node_error_published_not_found_for_uuid_release_language", nodeUuid, "de",
				releaseUuid);

		// Take english language offline
		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, nodeUuid, "en"));

		// The node should not be loadable since both languages are offline
		call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().published()), NOT_FOUND, "node_error_published_not_found_for_uuid_release_language", nodeUuid, "de",
				releaseUuid);

		// Publish german version
		PublishStatusModel publishStatus = call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"));
		assertThat(publishStatus).as("Publish status").isPublished().hasVersion("1.0");

		// Assert that german is published and english is offline
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new NodeParametersImpl().setLanguages("de"),
				new VersioningParametersImpl().published())).getAvailableLanguages()).containsOnly("de");
		assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid))).as("Publish status").isPublished("de").hasVersion("de", "1.0")
				.isNotPublished("en").hasVersion("en", "2.0");

	}

	@Test
	public void testPublishEmptyLanguage() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String nodeUuid = node.getUuid();
			call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de"), NOT_FOUND, "error_language_not_found", "de");
		}
	}

	@Test
	public void testPublishLanguageForRelease() {
		Project project = project();
		Node node = folder("2015");
		Release newRelease;

		try (Tx tx = tx()) {
			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(),
					new VersioningParametersImpl().setRelease(initialReleaseUuid()), new PublishParametersImpl().setRecursive(true)));

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("de");
			update.getFields().put("name", FieldUtil.createStringField("2015 de"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParametersImpl().setRelease(initialRelease().getName())));

			update.getFields().put("name", FieldUtil.createStringField("2015 new de"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParametersImpl().setRelease(newRelease.getName())));
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("2015 new en"));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParametersImpl().setRelease(newRelease.getName())));

			PublishStatusModel publishStatus = call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "de",
					new VersioningParametersImpl().setRelease(initialRelease().getName())));
			assertThat(publishStatus).isPublished();

			assertThat(call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid,
					new VersioningParametersImpl().setRelease(initialRelease().getName())))).as("Initial Release Publish Status").isPublished("de")
							.isNotPublished("en");
			assertThat(call(
					() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setRelease(newRelease.getName()))))
							.as("New Release Publish Status").isNotPublished("de").isNotPublished("en");
		}
	}

	@Test
	public void testPublishLanguageNoPermission() {
		Node node = folder("2015");
		try (Tx tx = tx()) {
			role().revokePermissions(node, PUBLISH_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();
			call(() -> client().publishNodeLanguage(PROJECT_NAME, nodeUuid, "en"), FORBIDDEN, "error_missing_perm", nodeUuid);
		}
	}

	@Test
	public void testPublishInOfflineContainer() {
		String nodeUuid = db().tx(() -> folder("2015").getUuid());

		// 1. Take a node subtree offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));

		// 2. Try to publish a node from within that subtree structure
		String contentUuid = db().tx(() -> content("news_2015").getUuid());
		call(() -> client().publishNode(PROJECT_NAME, contentUuid), BAD_REQUEST, "node_error_parent_containers_not_published", nodeUuid);

	}

	@Test
	public void testPublishRecursively() {
		String nodeUuid = db().tx(() -> project().getBaseNode().getUuid());
		String contentUuid = db().tx(() -> content("news_2015").getUuid());

		// 1. Check initial status
		assertPublishStatus("Node should be published.", nodeUuid, true);
		assertPublishStatus("Node should be published.", contentUuid, true);

		// 2. Take all nodes offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertPublishStatus("Node should be offline.", nodeUuid, false);
		assertPublishStatus("Node should be offline.", contentUuid, false);

		// 3. Publish all nodes again
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertPublishStatus("Node should be online again.", nodeUuid, true);
		assertPublishStatus("Node should be online again.", contentUuid, true);
	}

	@Test
	public void testPublishNoRecursion() {
		String nodeUuid = db().tx(() -> project().getBaseNode().getUuid());
		String contentUuid = db().tx(() -> content("news_2015").getUuid());

		// 1. Check initial status
		assertPublishStatus("Node should be published.", nodeUuid, true);
		assertPublishStatus("Node should be published.", contentUuid, true);

		// 2. Take all nodes offline
		call(() -> client().takeNodeOffline(PROJECT_NAME, nodeUuid, new PublishParametersImpl().setRecursive(true)));
		assertPublishStatus("Node should be offline.", nodeUuid, false);
		assertPublishStatus("Node should be offline.", contentUuid, false);

		// 3. Publish all nodes again
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));
		assertPublishStatus("Node should be online again.", nodeUuid, true);
		assertPublishStatus("Sub node should still be offline.", contentUuid, false);
	}

	private void assertPublishStatus(String message, String nodeUuid, boolean expectPublished) {
		PublishStatusResponse initialStatus = call(() -> client().getNodePublishStatus(PROJECT_NAME, nodeUuid));
		for (Entry<String, PublishStatusModel> entry : initialStatus.getAvailableLanguages().entrySet()) {
			if (expectPublished != entry.getValue().isPublished()) {
				fail("Publish status check for node {" + nodeUuid + "} failed for language {" + entry.getKey() + "} [" + message + "]");
			}
		}
	}

}
