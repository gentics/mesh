package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class NodeChildrenVerticleTest extends AbstractRestVerticleTest {

	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadChildrenOfBaseNode() {
		MeshResponse<NodeListResponse> future = getClient().findNodeChildren(PROJECT_NAME, project().getBaseNode().getUuid()).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	@Test
	public void testNodeHierarchy() {
		String parentNodeUuid;
		Node baseNode = project().getBaseNode();
		parentNodeUuid = baseNode.getUuid();
		NodeListResponse nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParameters().draft()));
		assertEquals(3, nodeList.getData().size());

		NodeCreateRequest create1 = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("folder");
		create1.setSchema(schemaReference);
		create1.setLanguage("en");
		create1.setParentNodeUuid(parentNodeUuid);
		NodeResponse createdNode = call(() -> getClient().createNode(PROJECT_NAME, create1));

		String uuid = createdNode.getUuid();
		nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		assertEquals(0, nodeList.getData().size());

		NodeCreateRequest create2 = new NodeCreateRequest();
		create2.setSchema(schemaReference);
		create2.setLanguage("en");
		create2.setParentNodeUuid(uuid);
		createdNode = call(() -> getClient().createNode(PROJECT_NAME, create2));

		nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		assertEquals("The subnode did not contain the created node", 1, nodeList.getData().size());

		nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParameters().draft()));
		assertEquals("The basenode should still contain four nodes.", 4, nodeList.getData().size());

	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		Node node = folder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());
		NodeResponse restNode = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
		test.assertMeshNode(node, restNode);
		assertTrue(restNode.isContainer());

		long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
		assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 2, subFolderCount);

		long subContentCount = restNode.getChildrenInfo().get("content").getCount();
		assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1, subContentCount);
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildrenPermissions() throws Exception {
		Node node = folder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		role().revokePermissions(folder("2015"), READ_PERM);

		NodeResponse restNode = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));
		test.assertMeshNode(node, restNode);
		assertTrue(restNode.isContainer());

		long subFolderCount = restNode.getChildrenInfo().get("folder").getCount();
		assertEquals("The node should have more than {" + subFolderCount + "} children. But it got {" + subFolderCount + "}", 1, subFolderCount);

		long subContentCount = restNode.getChildrenInfo().get("content").getCount();
		assertEquals("The node should have more than {" + subContentCount + "} children. But it got {" + subContentCount + "}", 1, subContentCount);
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		Node node = content("concorde");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		NodeResponse restNode = call(() -> getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new VersioningParameters().draft()));

		test.assertMeshNode(node, restNode);
		assertFalse("The node should not be a container", restNode.isContainer());
		assertNull(restNode.getChildrenInfo().get("folder"));
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		Node node = folder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		int expectedItemsInPage = node.getChildren().size() > 25 ? 25 : node.getChildren().size();

		NodeListResponse nodeList = call(
				() -> getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters(), new VersioningParameters().draft()));

		assertEquals(node.getChildren().size(), nodeList.getMetainfo().getTotalCount());
		assertEquals(expectedItemsInPage, nodeList.getData().size());
	}

	@Test
	public void testReadNodeChildrenWithoutChildPermission() throws Exception {
		Node node = folder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());
		Node nodeWithNoPerm = folder("2015");
		role().revokePermissions(nodeWithNoPerm, READ_PERM);

		NodeListResponse nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters().setPerPage(20000),
				new VersioningParameters().draft()));

		assertEquals(node.getChildren().size() - 1, nodeList.getMetainfo().getTotalCount());
		assertEquals(0, nodeList.getData().stream().filter(p -> nodeWithNoPerm.getUuid().equals(p.getUuid())).count());
		assertEquals(2, nodeList.getData().size());
	}

	@Test
	public void testReadNodeChildrenWithNoPermission() throws Exception {
		Node node = folder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		role().revokePermissions(node, READ_PERM);

		MeshResponse<NodeListResponse> future = getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters(),
				new NodeParameters()).invoke();
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", node.getUuid());

	}

	@Test
	public void testReadReleaseChildren() {
		Node node = folder("news");
		Node firstChild = node.getChildren().get(0);
		int childrenSize = node.getChildren().size();
		int expectedItemsInPage = childrenSize > 25 ? 25 : childrenSize;

		Project project = project();
		Release initialRelease = project.getInitialRelease();
		Release newRelease = project.getReleaseRoot().create("newrelease", user());

		NodeListResponse nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters(),
				new VersioningParameters().setRelease(initialRelease.getName()).draft()));
		assertEquals("Total children in initial release", childrenSize, nodeList.getMetainfo().getTotalCount());
		assertEquals("Returned children in initial release", expectedItemsInPage, nodeList.getData().size());

		nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters(),
				new VersioningParameters().setRelease(newRelease.getName()).draft()));
		assertEquals("Total children in initial release", 0, nodeList.getMetainfo().getTotalCount());
		assertEquals("Returned children in initial release", 0, nodeList.getData().size());

		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("name", FieldUtil.createStringField("new"));
		call(() -> getClient().updateNode(PROJECT_NAME, firstChild.getUuid(), update));

		nodeList = call(() -> getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingParameters(),
				new VersioningParameters().setRelease(newRelease.getName()).draft()));
		assertEquals("Total children in new release", 1, nodeList.getMetainfo().getTotalCount());
		assertEquals("Returned children in new release", 1, nodeList.getData().size());
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
