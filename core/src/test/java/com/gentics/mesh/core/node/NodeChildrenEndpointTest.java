package com.gentics.mesh.core.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectException;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeChildrenEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadChildrenOfBaseNode() {
		try (Tx tx = tx()) {
			call(() -> client().findNodeChildren(PROJECT_NAME, project().getBaseNode().getUuid()));
		}
	}

	@Test
	public void testNodeHierarchy() {
		try (Tx tx = tx()) {
			Node baseNode = project().getBaseNode();
			String parentNodeUuid = baseNode.getUuid();
			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().draft()));
			assertEquals(3, nodeList.getData().size());

			NodeCreateRequest create1 = new NodeCreateRequest();
			SchemaReference schemaReference = new SchemaReference();
			schemaReference.setName("folder");
			create1.setSchema(schemaReference);
			create1.setLanguage("en");
			create1.setParentNodeUuid(parentNodeUuid);
			NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, create1));

			String uuid = createdNode.getUuid();
			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			assertEquals(0, nodeList.getData().size());

			NodeCreateRequest create2 = new NodeCreateRequest();
			create2.setSchema(schemaReference);
			create2.setLanguage("en");
			create2.setParentNodeUuid(uuid);
			createdNode = call(() -> client().createNode(PROJECT_NAME, create2));

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().draft()));
			assertEquals("The subnode did not contain the created node", 1, nodeList.getData().size());

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().draft()));
			assertEquals("The basenode should still contain four nodes.", 4, nodeList.getData().size());
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertTrue(restNode.isContainer());

			long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
			assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 2, subFolderCount);

			long subContentCount = restNode.getChildrenInfo().get("content").getCount();
			assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1,
					subContentCount);
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildrenPermissions() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			role().revokePermissions(folder("2015"), READ_PERM);

			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertTrue(restNode.isContainer());

			long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
			assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 1, subFolderCount);

			long subContentCount = restNode.getChildrenInfo().get("content").getCount();
			assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1,
					subContentCount);
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		try (Tx tx = tx()) {
			Node node = content("concorde");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			NodeResponse restNode = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParametersImpl().draft()));
			assertThat(node).matches(restNode);
			assertFalse("The node should not be a container", restNode.isContainer());
			assertNull(restNode.getChildrenInfo().get("folder"));
		}
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			int expectedItemsInPage = node.getChildren().size() > 25 ? 25 : node.getChildren().size();

			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().draft()));

			assertEquals(node.getChildren().size(), nodeList.getMetainfo().getTotalCount());
			assertEquals(expectedItemsInPage, nodeList.getData().size());
		}
	}

	@Test
	public void testReadNodeChildrenWithoutChildPermission() throws Exception {
		try (Tx tx = tx()) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			Node nodeWithNoPerm = folder("2015");
			role().revokePermissions(nodeWithNoPerm, READ_PERM);

			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(),
					new PagingParametersImpl().setPerPage(20000), new VersioningParametersImpl().draft()));

			assertEquals(node.getChildren().size() - 1, nodeList.getMetainfo().getTotalCount());
			assertEquals(0, nodeList.getData().stream().filter(p -> nodeWithNoPerm.getUuid().equals(p.getUuid())).count());
			assertEquals(2, nodeList.getData().size());
		}
	}

	@Test
	public void testReadNodeChildrenWithNoPermission() throws Exception {
		Node node = folder("news");
		try (Tx tx = tx()) {
			assertNotNull(node);
			assertNotNull(node.getUuid());
			role().revokePermissions(node, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(), new NodeParametersImpl()), FORBIDDEN,
					"error_missing_perm", node.getUuid());
		}

	}

	@Test
	public void testReadReleaseChildren() {
		try (Tx tx = tx()) {
			Node node = folder("news");
			Node firstChild = node.getChildren().get(0);
			int childrenSize = node.getChildren().size();
			int expectedItemsInPage = childrenSize > 25 ? 25 : childrenSize;

			Project project = project();
			Release initialRelease = project.getInitialRelease();
			Release newRelease = project.getReleaseRoot().create("newrelease", user());

			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setRelease(initialRelease.getName()).draft()));
			assertEquals("Total children in initial release", childrenSize, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in initial release", expectedItemsInPage, nodeList.getData().size());

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setRelease(newRelease.getName()).draft()));
			assertEquals("Total children in initial release", 0, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in initial release", 0, nodeList.getData().size());

			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.getFields().put("name", FieldUtil.createStringField("new"));
			call(() -> client().updateNode(PROJECT_NAME, firstChild.getUuid(), update));

			nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParametersImpl(),
					new VersioningParametersImpl().setRelease(newRelease.getName()).draft()));
			assertEquals("Total children in new release", 1, nodeList.getMetainfo().getTotalCount());
			assertEquals("Returned children in new release", 1, nodeList.getData().size());
		}
	}

	@Test
	public void testFilterByLanguage() {
		String uuid = db().tx(() -> folder("2015").getUuid());

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("de");
		nodeCreateRequest.setParentNode(new NodeReference().setUuid(uuid));
		nodeCreateRequest.setSchema(new SchemaReference().setName("content"));
		nodeCreateRequest.getFields().put("teaser", new StringFieldImpl().setString("Only German Teaser"));
		nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString("Only German Slug"));

		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		NodeListResponse listResponse = call(() -> client().findNodeChildren(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("en")));
		List<String> langList = listResponse.getData().stream().map(node -> node.getLanguage()).collect(Collectors.toList());
		assertThat(langList).doesNotContain(null, "de");
	}

	@Test
	public void testReadPublishedChildren() {
		// TODO
	}

	@Test
	public void testReadReleasePublishedChildren() {
		// TODO
	}
}
