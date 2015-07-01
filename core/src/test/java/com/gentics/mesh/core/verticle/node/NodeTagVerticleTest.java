package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.NodeVerticle;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.DataHelper;

public class NodeTagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeVerticle verticle;

	@Autowired
	private NodeService nodeService;

	@Autowired
	private DataHelper helper;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testReadNodeTags() throws Exception {
		Node node = data().getFolder("2015");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags", 200, "OK");
		TagListResponse tagList = JsonUtil.readValue(response, TagListResponse.class);
		assertEquals(4, tagList.getData().size());
		assertEquals(4, tagList.getMetainfo().getTotalCount());

	}

	@Test
	public void testAddTagToNode() throws Exception {

		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		assertTrue(test.containsTag(restNode, tag));
		assertTrue(node.getTags().contains(tag));
		// TODO check for properties of the nested tag
	}

	@Test
	public void testAddTagToNoPermNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		info.getRole().revokePermissions(node, UPDATE_PERM);

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, node.getUuid());

		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testAddNoPermTagToNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("red");
		assertFalse(node.getTags().contains(tag));
		info.getRole().revokePermissions(tag, READ_PERM);

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, tag.getUuid());

		assertFalse(node.getTags().contains(tag));
	}

	@Test
	public void testRemoveTagFromNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");

		assertTrue(node.getTags().contains(tag));
		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		assertFalse(test.containsTag(restNode, tag));
		assertFalse(node.getTags().contains(tag));
		// TODO check for properties of the nested tag

	}

	@Test
	public void testRemoveBogusTagFromNode() throws Exception {
		Node node = data().getFolder("2015");

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/bogus", 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");
	}

	@Test
	public void testRemoveTagFromNoPermNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");
		assertTrue(node.getTags().contains(tag));
		info.getRole().revokePermissions(node, UPDATE_PERM);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, node.getUuid());

		assertTrue("The tag should not be removed from the node", node.getTags().contains(tag));
	}

	@Test
	public void testRemoveNoPermTagFromNode() throws Exception {
		Node node = data().getFolder("2015");
		Tag tag = data().getTag("bike");
		assertTrue(node.getTags().contains(tag));
		info.getRole().revokePermissions(tag, READ_PERM);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, tag.getUuid());

		// FramedVertexSet<Tag> tagSet = new FramedVertexSet<>(framedGraph, node.getTags(), Tag.class);
		assertTrue("The tag should not have been removed from the node", node.getTags().contains(tag));
	}

}
