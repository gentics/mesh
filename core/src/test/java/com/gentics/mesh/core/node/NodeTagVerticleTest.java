package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.performance.TestUtils;

import io.vertx.core.AbstractVerticle;

public class NodeTagVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.nodeVerticle());
		list.add(meshDagger.releaseVerticle());
		list.add(meshDagger.nodeMigrationVerticle());
		list.add(meshDagger.eventbusVerticle());
		list.add(meshDagger.tagFamilyVerticle());
		return list;
	}

	@Test
	public void testReadNodeTags() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			assertNotNull(node.getSchemaContainer());
			MeshResponse<TagListResponse> future = getClient().findTagsForNode(PROJECT_NAME, node.getUuid()).invoke();
			latchFor(future);
			assertSuccess(future);
			TagListResponse tagList = future.result();
			assertEquals(4, tagList.getData().size());
			assertEquals(4, tagList.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testAddTagToNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("red");
			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));

			assertThat(dummySearchProvider).recordedStoreEvents(0);
			MeshResponse<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()).invoke();
			latchFor(future);
			assertSuccess(future);
			assertThat(dummySearchProvider)
					.as("Recorded store events after node update occured. Published and draft of the node should have been updated.")
					.recordedStoreEvents(2);
			// node-[:nodeUuid]-[draft/published]-[schemaname]-[schemaversion]-[release_uuid]
			dummySearchProvider.getStoreEvents().containsKey("node-" + node.getUuid() + "-published-folder-1-" + project().getLatestRelease().getUuid());
			dummySearchProvider.getStoreEvents().containsKey("node-" + node.getUuid() + "-draft-folder-1-" + project().getLatestRelease().getUuid());

			future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()).invoke();
			latchFor(future);
			assertSuccess(future);

			node.reload();
			NodeResponse restNode = future.result();
			assertThat(restNode).contains(tag);
			assertTrue(node.getTags(project().getLatestRelease()).contains(tag));

			// TODO check for properties of the nested tag
		}
	}

	@Test
	public void testAddTagToNoPermNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("red");
			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));
			role().revokePermissions(node, UPDATE_PERM);

			MeshResponse<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());
			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));
		}
	}

	@Test
	public void testAddNoPermTagToNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("red");
			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));
			role().revokePermissions(tag, READ_PERM);

			MeshResponse<NodeResponse> future = getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());

			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));
		}
	}

	@Test
	public void testRemoveTagFromNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("bike");
			assertTrue(node.getTags(project().getLatestRelease()).contains(tag));

			call(() -> getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid()));

			NodeResponse restNode = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid()));
			assertThat(restNode).contains(tag);
			node.reload();
			assertFalse(node.getTags(project().getLatestRelease()).contains(tag));
			// TODO check for properties of the nested tag
		}
	}

	@Test
	public void testRemoveBogusTagFromNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> getClient().removeTagFromNode(PROJECT_NAME, uuid, "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	public void testRemoveTagFromNoPermNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("bike");
			assertTrue(node.getTags(project().getLatestRelease()).contains(tag));
			role().revokePermissions(node, UPDATE_PERM);

			call(() -> getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeParameters()), FORBIDDEN,
					"error_missing_perm", node.getUuid());

			assertTrue("The tag should not be removed from the node", node.getTags(project().getLatestRelease()).contains(tag));
		}
	}

	@Test
	public void testTaggingAcrossMultipleReleases() throws Exception {
		String releaseOne = "ReleaseV1";
		String releaseTwo = "ReleaseV2";

		// 1. Create release v1
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());
		try (NoTx noTx = db.noTx()) {
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseOne);
			ReleaseResponse releaseResponse = call(() -> getClient().createRelease(PROJECT_NAME, request));
			assertThat(releaseResponse).as("Release Response").isNotNull().hasName(releaseOne).isActive().isNotMigrated();
		}
		failingLatch(latch);

		// 2. Tag a node in release v1 with tag "red"
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			Tag tag = tag("red");
			call(() -> getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new VersioningParameters().setRelease(releaseOne)));
		}

		// Assert that the node is tagged with red in release one
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
					() -> getClient().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
					tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
					() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected to find the red tag in the node response", 1,
					response.getTags().get("colors").getItems().stream().filter(tag -> tag.getName().equals("red")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag = tag("red");
			NodeListResponse taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag.getTagFamily().getUuid(), tag.getUuid(),
					new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// 3. Create release v2
		latch = TestUtils.latchForMigrationCompleted(getClient());
		try (NoTx noTx = db.noTx()) {
			ReleaseCreateRequest request = new ReleaseCreateRequest();
			request.setName(releaseTwo);
			ReleaseResponse releaseResponse = call(() -> getClient().createRelease(PROJECT_NAME, request));
			assertThat(releaseResponse).as("Release Response").isNotNull().hasName(releaseTwo).isActive().isNotMigrated();
		}

		failingLatch(latch);
		// 4. Tag a node in release v2 with tag "blue"
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			Tag tag = tag("blue");
			call(() -> getClient().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new VersioningParameters().setRelease(releaseTwo)));
		}

		// Assert that the node is tagged with both tags in releaseTwo
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
					() -> getClient().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
					tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected the node to be tagged with the blue tag but the tag was not found in the list.", 1,
					tagsForNode.getData().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
					() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the red tag in the node response", 1,
					response.getTags().get("colors").getItems().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected to find the red tag in the node response", 1,
					response.getTags().get("colors").getItems().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag1 = tag("red");
			NodeListResponse taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag1.getTagFamily().getUuid(), tag1.getUuid(),
					new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

			Tag tag2 = tag("blue");
			taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag2.getTagFamily().getUuid(), tag2.getUuid(),
					new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// 5. Remove the tag "red" in release v1
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			Tag tag = tag("red");
			call(() -> getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new VersioningParameters().setRelease(releaseOne)));
		}

		// Assert that the node is still tagged with both tags in releaseTwo
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
					() -> getClient().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
					tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected the node to be tagged with the blue tag but the tag was not found in the list.", 1,
					tagsForNode.getData().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
					() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the red tag in the node response", 1,
					response.getTags().get("colors").getItems().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected to find the red tag in the node response", 1,
					response.getTags().get("colors").getItems().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag1 = tag("red");
			NodeListResponse taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag1.getTagFamily().getUuid(), tag1.getUuid(),
					new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

			Tag tag2 = tag("blue");
			taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag2.getTagFamily().getUuid(), tag2.getUuid(),
					new VersioningParameters().setRelease(releaseTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// Assert that the node is tagged with no tag in release one
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
					() -> getClient().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected to find no tags for the node in release one.", 0, tagsForNode.getData().size());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
					() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected to find no tags for the node in release one.", 0, response.getTags().size());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag = tag("red");
			NodeListResponse taggedNodes = call(() -> getClient().findNodesForTag(PROJECT_NAME, tag.getTagFamily().getUuid(), tag.getUuid(),
					new VersioningParameters().setRelease(releaseOne)));
			assertEquals("We expected to find the node not be tagged by tag red.", 0,
					taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

	}

	@Test
	public void testRemoveNoPermTagFromNode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			Tag tag = tag("bike");
			assertTrue(node.getTags(project().getLatestRelease()).contains(tag));
			role().revokePermissions(tag, READ_PERM);
			call(() -> getClient().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeParameters()), FORBIDDEN,
					"error_missing_perm", tag.getUuid());

			assertTrue("The tag should not have been removed from the node", node.getTags(project().getLatestRelease()).contains(tag));
		}
	}

}
