package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class NodeTagEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadNodeTags() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			assertNotNull(node.getSchemaContainer());
			TagListResponse tagList = call(() -> client().findTagsForNode(PROJECT_NAME, node.getUuid()));
			assertEquals(4, tagList.getData().size());
			assertEquals(4, tagList.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testAddTagToNode() throws Exception {
		Node node = folder("2015");
		String nodeUuid = tx(() -> node.getUuid());
		String schemaUuid = tx(() -> node.getSchemaContainer().getUuid());
		Tag tag = tag("red");
		String tagUuid = tx(() -> tag.getUuid());

		try (Tx tx = tx()) {
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
			assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		}

		expect(NODE_TAGGED).match(1, NodeTaggedEventModel.class, event -> {
			BranchReference branchRef = event.getBranch();
			assertNotNull(branchRef);
			assertEquals(initialBranchUuid(), branchRef.getUuid());
			assertEquals(PROJECT_NAME, branchRef.getName());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());

			NodeReference nodeRef = event.getNode();
			assertNotNull(nodeRef);
			assertEquals(nodeUuid, nodeRef.getUuid());

			SchemaReference schemaRef = nodeRef.getSchema();
			assertNotNull(schemaRef);
			assertEquals("folder", schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());

			TagReference tagRef = event.getTag();
			assertNotNull(tagRef);
			assertEquals("red", tagRef.getName());
			assertEquals(tagUuid, tagRef.getUuid());
		}).one();

		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagUuid));

		// Test idempotency
		NodeResponse restNode = call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tagUuid));
		awaitEvents();

		try (Tx tx = tx()) {
			assertThat(restNode).contains(tag);
			assertTrue(node.getTags(project().getLatestBranch()).list().contains(tag));
		}

		// TODO check for properties of the nested tag

	}

	@Test
	public void testAddTagToNoPermNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("red");
		try (Tx tx = tx()) {
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
			role().revokePermissions(node, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()), FORBIDDEN, "error_missing_perm", node.getUuid(),
				UPDATE_PERM.getRestPerm().getName());
		}

		try (Tx tx = tx()) {
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
		}
	}

	@Test
	public void testAddNoPermTagToNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("red");

		try (Tx tx = tx()) {
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
			role().revokePermissions(tag, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid()), FORBIDDEN, "error_missing_perm", tag.getUuid(),
				READ_PERM.getRestPerm().getName());
		}

		try (Tx tx = tx()) {
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
		}
	}

	@Test
	public void testUntaggingViaDeleteOfTag() {
		Node node = folder("2015");
		Tag tag = tag("bike");
		String schemaUuid = tx(() -> node.getSchemaContainer().getUuid());
		String nodeUuid = tx(() -> node.getUuid());
		String tagUuid = tx(() -> tag.getUuid());
		String tagFamilyUuid = tx(() -> tag.getTagFamily().getUuid());

		try (Tx tx = tx()) {
			assertTrue(node.getTags(project().getLatestBranch()).list().contains(tag));
		}

		expect(NODE_UNTAGGED).match(1, NodeTaggedEventModel.class, event -> {
			BranchReference branchRef = event.getBranch();
			assertNotNull(branchRef);
			assertEquals(initialBranchUuid(), branchRef.getUuid());
			assertEquals(PROJECT_NAME, branchRef.getName());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());

			NodeReference nodeRef = event.getNode();
			assertNotNull(nodeRef);
			assertEquals(nodeUuid, nodeRef.getUuid());

			SchemaReference schemaRef = nodeRef.getSchema();
			assertNotNull(schemaRef);
			assertEquals("folder", schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());

			TagReference tagRef = event.getTag();
			assertNotNull(tagRef);
			assertEquals("Bike", tagRef.getName());
			assertEquals(tagUuid, tagRef.getUuid());
		}).one();

		// Now delete the tag and assert the events
		call(() -> client().deleteTag(projectName(), tagFamilyUuid, tagUuid));

		awaitEvents();
		waitForSearchIdleEvent();

		long storeEvents = 2; // Draft and published documents
		long deleteEvents = 1; // One tag
		assertThat(trackingSearchProvider()).hasEvents(storeEvents, deleteEvents, 0, 0);

	}

	@Test
	public void testRemoveTagFromNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");
		String schemaUuid = tx(() -> node.getSchemaContainer().getUuid());
		String nodeUuid = tx(() -> node.getUuid());
		String tagUuid = tx(() -> tag.getUuid());
		try (Tx tx = tx()) {
			assertTrue(node.getTags(project().getLatestBranch()).list().contains(tag));
		}

		expect(NODE_UNTAGGED).match(1, NodeTaggedEventModel.class, event -> {
			BranchReference branchRef = event.getBranch();
			assertNotNull(branchRef);
			assertEquals(initialBranchUuid(), branchRef.getUuid());
			assertEquals(PROJECT_NAME, branchRef.getName());

			ProjectReference projectRef = event.getProject();
			assertNotNull(projectRef);
			assertEquals(PROJECT_NAME, projectRef.getName());
			assertEquals(projectUuid(), projectRef.getUuid());

			NodeReference nodeRef = event.getNode();
			assertNotNull(nodeRef);
			assertEquals(nodeUuid, nodeRef.getUuid());

			SchemaReference schemaRef = nodeRef.getSchema();
			assertNotNull(schemaRef);
			assertEquals("folder", schemaRef.getName());
			assertEquals(schemaUuid, schemaRef.getUuid());

			TagReference tagRef = event.getTag();
			assertNotNull(tagRef);
			assertEquals("Bike", tagRef.getName());
			assertEquals(tagUuid, tagRef.getUuid());
		}).one();

		call(() -> client().removeTagFromNode(PROJECT_NAME, nodeUuid, tagUuid));

		// Test for idempotency
		call(() -> client().removeTagFromNode(PROJECT_NAME, nodeUuid, tagUuid));

		awaitEvents();
		NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid));

		try (Tx tx = tx()) {
			assertThat(restNode).contains(tag);
			assertFalse(node.getTags(project().getLatestBranch()).list().contains(tag));
		}
		// TODO check for properties of the nested tag
	}

	@Test
	public void testRemoveBogusTagFromNode() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			String uuid = node.getUuid();

			call(() -> client().removeTagFromNode(PROJECT_NAME, uuid, "bogus"), NOT_FOUND, "object_not_found_for_uuid",
				"bogus");
		}
	}

	@Test
	public void testRemoveTagFromNoPermNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");

		try (Tx tx = tx()) {
			assertTrue(node.getTags(project().getLatestBranch()).list().contains(tag));
			role().revokePermissions(node, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeParametersImpl()), FORBIDDEN,
				"error_missing_perm", node.getUuid(), UPDATE_PERM.getRestPerm().getName());
			assertTrue("The tag should not be removed from the node", node.getTags(project().getLatestBranch()).list().contains(tag));
		}
	}

	@Test
	public void testTaggingAcrossMultipleBranches() throws Exception {
		grantAdminRole();
		String branchOne = "BranchV1";
		String branchTwo = "BranchV2";

		// 1. Create branch v1
		waitForLatestJob(() -> {
			try (Tx tx = tx()) {
				BranchCreateRequest request = new BranchCreateRequest();
				request.setName(branchOne);
				BranchResponse branchResponse = call(() -> client().createBranch(PROJECT_NAME, request));
				assertThat(branchResponse).as("Branch Response").isNotNull().hasName(branchOne).isActive().isNotMigrated();
			}
		});

		// 2. Tag a node in branch v1 with tag "red"
		try (Tx tx = tx()) {
			Node node = content();
			Tag tag = tag("red");
			call(() -> client().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new VersioningParametersImpl().setBranch(branchOne)));
		}

		// Assert that the node is tagged with red in branch one
		try (Tx tx = tx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
				() -> client().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
				tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
				() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected to find the red tag in the node response", 1,
				response.getTags().stream().filter(tag -> tag.getName().equals("red")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag = tag("red");
			NodeListResponse taggedNodes = call(() -> client().findNodesForTag(PROJECT_NAME, tag.getTagFamily().getUuid(), tag.getUuid(),
				new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// 3. Create branch v2
		waitForLatestJob(() -> {
			try (Tx tx = tx()) {
				BranchCreateRequest request = new BranchCreateRequest();
				request.setName(branchTwo);
				BranchResponse branchResponse = call(() -> client().createBranch(PROJECT_NAME, request));
				assertThat(branchResponse).as("Branch Response").isNotNull().hasName(branchTwo).isActive().isNotMigrated();
			}
		});

		// 4. Tag a node in branch v2 with tag "blue"
		try (Tx tx = tx()) {
			Node node = content();
			Tag tag = tag("blue");
			call(() -> client().addTagToNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
		}

		// Assert that the node is tagged with both tags in branchTwo
		try (Tx tx = tx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
				() -> client().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
				tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected the node to be tagged with the blue tag but the tag was not found in the list.", 1,
				tagsForNode.getData().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
				() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the red tag in the node response", 1,
				response.getTags().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected to find the red tag in the node response", 1,
				response.getTags().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag1 = tag("red");
			NodeListResponse taggedNodes = call(() -> client().findNodesForTag(PROJECT_NAME, tag1.getTagFamily().getUuid(), tag1.getUuid(),
				new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

			Tag tag2 = tag("blue");
			taggedNodes = call(() -> client().findNodesForTag(PROJECT_NAME, tag2.getTagFamily().getUuid(), tag2.getUuid(),
				new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// 5. Remove the tag "red" in branch v1
		try (Tx tx = tx()) {
			Node node = content();
			Tag tag = tag("red");
			call(() -> client().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(),
				new VersioningParametersImpl().setBranch(branchOne)));
		}

		// Assert that the node is still tagged with both tags in branchTwo
		try (Tx tx = tx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(
				() -> client().findTagsForNode(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected the node to be tagged with the red tag but the tag was not found in the list.", 1,
				tagsForNode.getData().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected the node to be tagged with the blue tag but the tag was not found in the list.", 1,
				tagsForNode.getData().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /nodes/:nodeUuid
			NodeResponse response = call(
				() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the red tag in the node response", 1,
				response.getTags().stream().filter(tag -> tag.getName().equals("red")).count());
			assertEquals("We expected to find the red tag in the node response", 1,
				response.getTags().stream().filter(tag -> tag.getName().equals("blue")).count());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag1 = tag("red");
			NodeListResponse taggedNodes = call(
				() -> client().findNodesForTag(PROJECT_NAME, tag1.getTagFamily().getUuid(), tag1.getUuid(),
					new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

			Tag tag2 = tag("blue");
			taggedNodes = call(() -> client().findNodesForTag(PROJECT_NAME, tag2.getTagFamily().getUuid(),
				tag2.getUuid(), new VersioningParametersImpl().setBranch(branchTwo)));
			assertEquals("We expected to find the node in the list response but it was not included.", 1,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

		// Assert that the node is tagged with no tag in branch one
		try (Tx tx = tx()) {
			Node node = content();
			// via /nodes/:nodeUuid/tags
			TagListResponse tagsForNode = call(() -> client().findTagsForNode(PROJECT_NAME, node.getUuid(),
				new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected to find no tags for the node in branch one.", 0, tagsForNode.getData().size());

			// via /nodes/:nodeUuid
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(),
				new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected to find no tags for the node in branch one.", 0, response.getTags().size());

			// via /tagFamilies/:tagFamilyUuid/tags/:tagUuid/nodes
			Tag tag = tag("red");
			NodeListResponse taggedNodes = call(() -> client().findNodesForTag(PROJECT_NAME,
				tag.getTagFamily().getUuid(), tag.getUuid(), new VersioningParametersImpl().setBranch(branchOne)));
			assertEquals("We expected to find the node not be tagged by tag red.", 0,
				taggedNodes.getData().stream().filter(item -> item.getUuid().equals(node.getUuid())).count());

		}

	}

	@Test
	public void testRemoveNoPermTagFromNode() throws Exception {
		Node node = folder("2015");
		Tag tag = tag("bike");

		try (Tx tx = tx()) {
			assertTrue(node.getTags(project().getLatestBranch()).list().contains(tag));
			role().revokePermissions(tag, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().removeTagFromNode(PROJECT_NAME, node.getUuid(), tag.getUuid(), new NodeParametersImpl()), FORBIDDEN,
				"error_missing_perm", tag.getUuid(), READ_PERM.getRestPerm().getName());
		}

		try (Tx tx = tx()) {
			assertTrue("The tag should not have been removed from the node",
				node.getTags(project().getLatestBranch()).list().contains(tag));
		}
	}

	@Test
	public void testCreateNodeWithTags() {
		NodeCreateRequest request = prepareCreateRequest();

		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setTagFamily("colors"));
		request.setTags(tags);

		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		List<TagReference> loadedTags = response.getTags();
		assertThat(loadedTags).isNotEmpty().hasSize(1);
		TagReference tag = loadedTags.get(0);
		assertEquals("red", tag.getName());
		assertEquals("colors", tag.getTagFamily());
	}

	/**
	 * Test create a node which lists at least one tag which has not yet been created.
	 */
	@Test
	public void testCreateNodeWithNewTags() {
		NodeCreateRequest request = prepareCreateRequest();

		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setTagFamily("colors"));
		tags.add(new TagReference().setName("red123").setTagFamily("colors"));
		request.setTags(tags);

		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
		List<TagReference> loadedTags = response.getTags();
		assertThat(loadedTags).isNotEmpty().hasSize(2);

		TagReference tag = loadedTags.get(0);
		assertEquals("red", tag.getName());
		assertEquals("colors", tag.getTagFamily());

		TagReference tag2 = loadedTags.get(1);
		assertEquals("red123", tag2.getName());
		assertEquals("colors", tag2.getTagFamily());
	}

	private NodeCreateRequest prepareCreateRequest() {
		String folderUuid = tx(() -> folder("2015").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid(folderUuid);
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setSchemaName("content");
		return request;
	}

	@Test
	public void testUpdateNodeWithTags() {
		NodeUpdateRequest request = prepareUpdateRequest();
		String nodeUuid = contentUuid();

		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setTagFamily("colors"));
		request.setTags(tags);

		NodeResponse response = call(() -> client().updateNode(PROJECT_NAME, nodeUuid, request));
		List<TagReference> loadedTags = response.getTags();
		assertThat(loadedTags).isNotEmpty().hasSize(1);
		TagReference tag = loadedTags.get(0);
		assertEquals("red", tag.getName());
		assertEquals("colors", tag.getTagFamily());
	}

	@Test
	public void testUpdateNodeWithNewTags() {
		NodeUpdateRequest request = prepareUpdateRequest();
		String nodeUuid = contentUuid();

		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("red").setTagFamily("colors"));
		tags.add(new TagReference().setName("red1234").setTagFamily("colors"));
		request.setTags(tags);

		NodeResponse response = call(() -> client().updateNode(PROJECT_NAME, nodeUuid, request));
		List<TagReference> loadedTags = response.getTags();
		assertThat(loadedTags).isNotEmpty().hasSize(2);

		TagReference tag = loadedTags.get(0);
		assertEquals("red", tag.getName());
		assertEquals("colors", tag.getTagFamily());

		TagReference tag2 = loadedTags.get(1);
		assertEquals("red1234", tag2.getName());
		assertEquals("colors", tag2.getTagFamily());
	}

	private NodeUpdateRequest prepareUpdateRequest() {
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("en");
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setVersion("0.1");
		return request;
	}

}
