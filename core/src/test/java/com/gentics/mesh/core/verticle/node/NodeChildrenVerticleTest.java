package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.verticle.NodeVerticle;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.DataHelper;

public class NodeChildrenVerticleTest extends AbstractRestVerticleTest {

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
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		Node node = data().getFolder("2015");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);
		assertTrue(restNode.isContainer());
		assertTrue(restNode.getChildren().size() > 5);
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		Node node = data().getContent("boeing 737");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtil.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);
		assertFalse(restNode.isContainer());
		assertNull(restNode.getChildren());
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		Node node = data().getFolder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		int expectedItemsInPage = node.getChildren().size() > 25 ? 25 : node.getChildren().size();
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/children", 200, "OK");
		NodeListResponse nodeList = JsonUtil.readValue(response, NodeListResponse.class);
		assertEquals(node.getChildren().size(), nodeList.getMetainfo().getTotalCount());
		assertEquals(expectedItemsInPage, nodeList.getData().size());
	}

}
