package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeTagUpdateEndpointTest extends AbstractMeshTest {

	@Test
	public void testMissingTagFamilyName() {
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("green").setTagFamily(""));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), BAD_REQUEST, "tag_error_tagfamily_not_set");
		assertThat(trackingSearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testUnknownTagFamilyName() {
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("green").setTagFamily("blub123"));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), NOT_FOUND, "tagfamily_not_found", "blub123");
		assertThat(trackingSearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testMissingTagName() {
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("").setTagFamily("colors"));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), BAD_REQUEST, "tag_error_name_or_uuid_missing");
		assertThat(trackingSearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testUpdateByTagUuid() {
		long previousCount = tx(() -> tagFamily("colors").findAll().count());
		String tagUuid = tx(() -> tag("red").getUuid());
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setUuid(tagUuid).setTagFamily("colors"));
		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(1, response.getMetainfo().getTotalCount());
		long afterCount = tx(() -> tagFamily("colors").findAll().count());
		assertEquals("The colors tag family should not have any additional tags.", previousCount, afterCount);

		waitForSearchIdleEvent();
		try (Tx tx = tx()) {
			assertThat(trackingSearchProvider()).storedAllContainers(content(), project(), latestBranch(), "en", "de").hasEvents(4, 0, 0, 0, 0);
		}

	}

	@Test
	public void testUpdateByTagName() {
		long previousCount = tx(() -> tagFamily("colors").findAll().count());
		assertEquals("The colors tag family did not have the expected amount of tags", 3, previousCount);

		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String branchUuid = tx(() -> initialBranchUuid());
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("purple").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("red").setTagFamily("colors"));

		expect(TAG_CREATED).match(1, TagMeshEventModel.class, event -> {
			assertEquals("purple", event.getName());
			assertNotNull(event.getUuid());
			TagFamilyReference tagFamilyRef = event.getTagFamily();
			assertEquals("colors", tagFamilyRef.getName());
			assertEquals(tagFamilyUuid, tagFamilyRef.getUuid());
		}).one();
		expect(TAG_DELETED).none();
		expect(NODE_UNTAGGED).none();
		expect(TAG_FAMILY_UPDATED).none();
		expect(NODE_TAGGED).match(2, NodeTaggedEventModel.class, event -> {
			assertThat(event.getTag().getName()).matches("red|purple");
			BranchReference branchRef = event.getBranch();
			assertEquals(PROJECT_NAME, branchRef.getName());
			assertEquals(branchUuid, branchRef.getUuid());

			NodeReference nodeRef = event.getNode();
			assertEquals(nodeUuid, nodeRef.getUuid());
		}).two();

		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		awaitEvents();
		waitForSearchIdleEvent();

		assertEquals("The node should have two tags.", 2, response.getMetainfo().getTotalCount());
		long afterCount = tx(() -> tagFamily("colors").findAll().count());
		assertEquals("The colors tag family should now have one additional color tag.", previousCount + 1, afterCount);

		try (Tx tx = tx()) {
			assertThat(trackingSearchProvider()).storedAllContainers(content(), project(), latestBranch(), "en", "de");
			assertThat(trackingSearchProvider()).stored(tagFamily("colors"));
			assertThat(trackingSearchProvider()).stored(tagFamily("colors").findByName("purple"));
			// 4( contents (en,de * type) + 1 new tag + 1 tagfamily doc updated
			long stores = 6;
			assertThat(trackingSearchProvider()).hasEvents(stores, 0, 0, 0, 0);
		}

		// Test for idempotency
		trackingSearchProvider().reset();
		expect(TAG_DELETED).none();
		expect(TAG_CREATED).none();
		expect(NODE_TAGGED).none();
		expect(NODE_UNTAGGED).none();
		expect(TAG_FAMILY_UPDATED).none();
		response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		awaitEvents();
		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);

		// Test removal of tag
		expect(TAG_CREATED).none();
		expect(TAG_DELETED).none();
		expect(NODE_TAGGED).none();
		expect(TAG_FAMILY_UPDATED).none();
		expect(NODE_UNTAGGED).match(1, NodeTaggedEventModel.class, event -> {
			assertThat(event.getTag().getName()).matches("purple");
			BranchReference branchRef = event.getBranch();
			assertEquals(PROJECT_NAME, branchRef.getName());
			assertEquals(branchUuid, branchRef.getUuid());

			NodeReference nodeRef = event.getNode();
			assertEquals(nodeUuid, nodeRef.getUuid());
		}).one();

		TagListUpdateRequest request2 = new TagListUpdateRequest();
		request2.getTags().add(new TagReference().setName("red").setTagFamily("colors"));
		response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request2));
		awaitEvents();
		waitForSearchIdleEvent();

		// 4 content docs (en,de * type)
		long stores = 4;
		assertThat(trackingSearchProvider()).hasEvents(stores, 0, 0, 0, 0);

	}

	@Test
	public void testUpdateWithNewTagFamilyAndTag() {

		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla4").setTagFamily("basic"));

		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(4, response.getMetainfo().getTotalCount());
		waitForSearchIdleEvent();
		try (Tx tx = tx()) {
			// 4 Node containers need to be updated and two tag families and 4 new tags
			assertThat(trackingSearchProvider()).storedAllContainers(content(), project(), latestBranch(), "en", "de").hasEvents(4 + 2 + 4, 0, 0, 0, 0);
		}

		trackingSearchProvider().clear().blockingAwait();
		request.getTags().clear();
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(2, response.getMetainfo().getTotalCount());
		waitForSearchIdleEvent();
		try (Tx tx = tx()) {
			// No tag family is modified - no Tag is created
			assertThat(trackingSearchProvider()).storedAllContainers(content(), project(), latestBranch(), "en", "de").hasEvents(4, 0, 0, 0, 0);
		}

	}

	@Test
	public void testTagOrder() {

		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla4").setTagFamily("basic"));

		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertThat(response).containsExactly("blub1", "bla2", "blub3", "bla4");

		request.getTags().clear();
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertThat(response).containsExactly("bla2", "blub3");
	}

	@Test
	public void testUpdateWithNoNodePerm() {
		// 1. Revoke the update permission
		try (Tx tx = tx()) {
			role().revokePermissions(content(), UPDATE_PERM);
			tx.success();
		}
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		// 2. Invoke the tag request
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), FORBIDDEN, "error_missing_perm", nodeUuid,
			UPDATE_PERM.getRestPerm().getName());
		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);

	}

	@Test
	public void testUpdateWithNoTagFamilyCreatePerm() {
		// 1. Revoke the tag create permission
		try (Tx tx = tx()) {
			role().revokePermissions(tagFamily("colors"), CREATE_PERM);
			tx.success();
		}
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String nodeUuid = tx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		// 2. Invoke the tag request
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), FORBIDDEN, "tag_error_missing_perm_on_tag_family", "colors",
			tagFamilyUuid, "blub1");
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);
	}
}
