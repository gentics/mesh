package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeTagUpdateEndpointTest extends AbstractMeshTest {

	@Test
	public void testMissingTagFamilyName() {
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("green").setTagFamily(""));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), BAD_REQUEST, "tag_error_tagfamily_not_set");
		assertThat(dummySearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testUnknownTagFamilyName() {
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("green").setTagFamily("blub123"));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), NOT_FOUND, "object_not_found_for_name", "blub123");
		assertThat(dummySearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testMissingTagName() {
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("").setTagFamily("colors"));
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), BAD_REQUEST, "tag_error_name_or_uuid_missing");
		assertThat(dummySearchProvider()).hasNoStoreEvents();
	}

	@Test
	public void testUpdateByTagUuid() {
		int previousCount = db().noTx(() -> tagFamily("colors").findAll().size());
		String tagUuid = db().noTx(() -> tag("red").getUuid());
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setUuid(tagUuid).setTagFamily("colors"));
		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(1, response.getMetainfo().getTotalCount());
		int afterCount = db().noTx(() -> tagFamily("colors").findAll().size());
		assertEquals("The colors tag family should not have any additional tags.", previousCount, afterCount);

		try (NoTx noTx = db().noTx()) {
			assertThat(dummySearchProvider()).storedAllContainers(content(), this, "en", "de").hasEvents(4, 0, 0, 0);
		}

	}

	@Test
	public void testUpdateByTagName() {
		int previousCount = db().noTx(() -> tagFamily("colors").findAll().size());
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("purple").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("red").setTagFamily("colors"));
		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals("The node should have two tags.", 2, response.getMetainfo().getTotalCount());
		int afterCount = db().noTx(() -> tagFamily("colors").findAll().size());
		assertEquals("The colors tag family should now have one additional color tag.", previousCount + 1, afterCount);

		try (NoTx noTx = db().noTx()) {
			assertThat(dummySearchProvider()).storedAllContainers(content(), this, "en", "de");
			assertThat(dummySearchProvider()).stored(tagFamily("colors"));
			assertThat(dummySearchProvider()).stored(tagFamily("colors").findByName("purple"));
			assertThat(dummySearchProvider()).hasEvents(6, 0, 0, 0);
		}
	}

	@Test
	public void testUpdateWithNewTagFamilyAndTag() {

		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		request.getTags().add(new TagReference().setName("bla4").setTagFamily("basic"));

		TagListResponse response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(4, response.getMetainfo().getTotalCount());
		try (NoTx noTx = db().noTx()) {
			// 4 Node containers need to be updated and two tag families and 4 new tags
			assertThat(dummySearchProvider()).storedAllContainers(content(), this, "en", "de").hasEvents(4 + 2 + 4, 0, 0, 0);
		}

		dummySearchProvider().clear();
		request.getTags().clear();
		request.getTags().add(new TagReference().setName("bla2").setTagFamily("basic"));
		request.getTags().add(new TagReference().setName("blub3").setTagFamily("colors"));
		response = call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request));
		assertEquals(2, response.getMetainfo().getTotalCount());
		try (NoTx noTx = db().noTx()) {
			// No tag family is modified - no Tag is created
			assertThat(dummySearchProvider()).storedAllContainers(content(), this, "en", "de").hasEvents(4, 0, 0, 0);
		}

	}

	@Test
	public void testTagOrder() {

		String nodeUuid = db().noTx(() -> content().getUuid());
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
		try (NoTx noTx = db().noTx()) {
			role().revokePermissions(content(), UPDATE_PERM);
		}
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		// 2. Invoke the tag request
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), FORBIDDEN, "error_missing_perm", nodeUuid);
		assertThat(dummySearchProvider()).hasEvents(0, 0, 0, 0);

	}

	@Test
	public void testUpdateWithNoTagFamilyCreatePerm() {
		// 1. Revoke the tag create permission
		try (NoTx noTx = db().noTx()) {
			role().revokePermissions(tagFamily("colors"), CREATE_PERM);
		}
		String tagFamilyUuid = db().noTx(() -> tagFamily("colors").getUuid());
		String nodeUuid = db().noTx(() -> content().getUuid());
		TagListUpdateRequest request = new TagListUpdateRequest();
		request.getTags().add(new TagReference().setName("blub1").setTagFamily("colors"));
		// 2. Invoke the tag request
		call(() -> client().updateTagsForNode(PROJECT_NAME, nodeUuid, request), FORBIDDEN, "tag_error_missing_perm_on_tag_family", "colors",
				tagFamilyUuid, "blub1");
		assertThat(dummySearchProvider()).hasEvents(0, 0, 0, 0);
	}
}
