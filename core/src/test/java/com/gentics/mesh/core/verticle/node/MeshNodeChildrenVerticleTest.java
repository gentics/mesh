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
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.rest.node.response.NodeListResponse;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.verticle.MeshNodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.DataHelper;
import com.gentics.mesh.util.JsonUtils;

public class MeshNodeChildrenVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private MeshNodeVerticle verticle;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private DataHelper helper;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren() throws Exception {
		MeshNode node = data().getFolder("2015");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtils.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);
		assertTrue(restNode.isContainer());
		assertTrue(restNode.getChildren().size() > 5);
	}

	@Test
	public void testReadNodeByUUIDAndCheckChildren2() throws Exception {
		MeshNode node = data().getContent("boeing 737");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid(), 200, "OK");
		NodeResponse restNode = JsonUtils.readValue(response, NodeResponse.class);
		test.assertMeshNode(node, restNode);
		assertFalse(restNode.isContainer());
		assertNull(restNode.getChildren());
	}

	@Test
	public void testReadNodeChildren() throws Exception {
		MeshNode node = data().getFolder("news");
		assertNotNull(node);
		assertNotNull(node.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/nodes/" + node.getUuid() + "/children", 200, "OK");
		NodeListResponse nodeList = JsonUtils.readValue(response, NodeListResponse.class);
		assertEquals(2, nodeList.getData().size());
		assertEquals(2, nodeList.getMetainfo().getTotalCount());
	}

}
