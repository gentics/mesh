package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.project.ProjectNodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class ProjectNodeChildrenVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectNodeVerticle verticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testReadChildrenOfBaseNode() {
		try (Trx tx = new Trx(db)) {
			Future<NodeListResponse> future = getClient().findNodeChildren(PROJECT_NAME, project().getBaseNode().getUuid());
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	public void testNodeHierarchy() {
		String parentNodeUuid;
		try (Trx tx = new Trx(db)) {
			Node baseNode = data().getProject().getBaseNode();
			parentNodeUuid = baseNode.getUuid();
		}
		Future<NodeListResponse> future = getClient().findNodeChildren(PROJECT_NAME, parentNodeUuid);
		latchFor(future);
		assertSuccess(future);
		assertEquals(3, future.result().getData().size());

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("folder");
		nodeCreateRequest.setSchema(schemaReference);
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		Future<NodeResponse> nodeCreateFuture = getClient().createNode(PROJECT_NAME, nodeCreateRequest);
		latchFor(nodeCreateFuture);
		assertSuccess(nodeCreateFuture);

		String uuid = nodeCreateFuture.result().getUuid();
		future = getClient().findNodeChildren(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());

		nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchema(schemaReference);
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(uuid);
		nodeCreateFuture = getClient().createNode(PROJECT_NAME, nodeCreateRequest);
		latchFor(nodeCreateFuture);
		assertSuccess(nodeCreateFuture);

		future = getClient().findNodeChildren(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
		assertEquals("The subnode did not contain the created node", 1, future.result().getData().size());

		future = getClient().findNodeChildren(PROJECT_NAME, parentNodeUuid);
		latchFor(future);
		assertSuccess(future);
		assertEquals("The basenode should still contain four nodes.", 4, future.result().getData().size());

	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			assertNotNull(node);
			assertNotNull(node.getUuid());
			Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
			latchFor(future);
			assertSuccess(future);
			NodeResponse restNode = future.result();
			test.assertMeshNode(node, restNode);
			assertTrue(restNode.isContainer());
			assertTrue(restNode.getChildren().size() > 5);
		}
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		Node node;
		try (Trx tx = new Trx(db)) {
			node = content("boeing 737");
			assertNotNull(node);
			assertNotNull(node.getUuid());
		}

		Future<NodeResponse> future = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
		latchFor(future);
		assertSuccess(future);
		NodeResponse restNode = future.result();

		try (Trx tx = new Trx(db)) {
			test.assertMeshNode(node, restNode);
			assertFalse(restNode.isContainer());
			assertNull(restNode.getChildren());
		}
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		try (Trx tx = new Trx(db)) {
			Node node = folder("news");
			assertNotNull(node);
			assertNotNull(node.getUuid());

			int expectedItemsInPage = node.getChildren().size() > 25 ? 25 : node.getChildren().size();

			Future<NodeListResponse> future = getClient().findNodeChildren(PROJECT_NAME, node.getUuid(), new PagingInfo(),
					new NodeRequestParameters());
			latchFor(future);
			assertSuccess(future);

			NodeListResponse nodeList = future.result();
			assertEquals(node.getChildren().size(), nodeList.getMetainfo().getTotalCount());
			assertEquals(expectedItemsInPage, nodeList.getData().size());
		}
	}

}
