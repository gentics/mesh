package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodePublishVerticleTests extends AbstractIsolatedRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	@Autowired
	private NodeVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testPublishNodeInUnpublishedContainer() {

		String folderUuid = db.noTrx(() -> {
			// 1. Take the parent folder offline
			InternalActionContext ac = getMockedInternalActionContext("");
			Node folder = folder("2015");
			folder.takeOffline(ac);
			folder("news").publish(ac);
			return folder.getUuid();
		});

		// 2. Create a new node in the folder
		NodeCreateRequest requestA = new NodeCreateRequest();
		requestA.setLanguage("en");
		requestA.setParentNodeUuid(folderUuid);
		requestA.setSchema(new SchemaReference().setName("content"));
		requestA.getFields().put("name", FieldUtil.createStringField("nodeA"));
		requestA.getFields().put("filename", FieldUtil.createStringField("nodeA"));
		NodeResponse nodeA = call(() -> getClient().createNode(PROJECT_NAME, requestA));

		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()), BAD_REQUEST, "node_error_parent_containers_not_published");

		// 3. Publish the folder
		call(() -> getClient().publishNode(PROJECT_NAME, folderUuid));

		// 4. Verify that publishing now works
		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()));

	}

	/**
	 * Verify that the takeOffline action fails if the node still has published children.
	 */
	@Test
	public void testTakeOfflineConsistency() {
		fail("implement me");
	}

	/**
	 * Verify that the move action fails if the published node is moved into offline containers.
	 */
	@Test
	public void testMoveConsistency() {
		fail("implement me");
	}

	/**
	 * Verify that the publish consistency is maintained per release.
	 */
	@Test
	public void testCrossReleasePublishConsistency() {
		fail("implement me");
	}

}
