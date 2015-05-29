package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.rest.node.response.NodeListResponse;
import com.gentics.mesh.core.verticle.MeshNodeVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.DataHelper;
import com.gentics.mesh.util.JsonUtils;

@Transactional(readOnly = true)
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
