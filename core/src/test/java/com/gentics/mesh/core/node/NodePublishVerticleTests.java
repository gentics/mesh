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
import com.gentics.mesh.query.impl.TakeOfflineParameter;
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

	/**
	 * Folder /news/2015 is not published. A new node will be created in folder 2015. Publishing the created folder should fail since the parent folder
	 * (/news/2015) is not yet published. This test will also assert that publishing works fine as soon as the parent node is published.
	 */
	@Test
	public void testPublishNodeInUnpublishedContainer() {

		// 1. Take the parent folder offline
		String folderUuid = db.noTrx(() -> {
			InternalActionContext ac = getMockedInternalActionContext("recursive=true");
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

		// 3. Publish nodeA - It should fail since the parentfolder is not published
		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()), BAD_REQUEST, "node_error_parent_containers_not_published", folderUuid);

		// 4. Publish the parent folder
		call(() -> getClient().publishNode(PROJECT_NAME, folderUuid));

		// 4. Verify that publishing now works
		call(() -> getClient().publishNode(PROJECT_NAME, nodeA.getUuid()));

	}

	/**
	 * Verify that the takeOffline action fails if the node still has published children.
	 */
	@Test
	public void testTakeNodeOfflineConsistency() {

		//1. Publish /news  & /news/2015
		db.noTrx(() -> {
			System.out.println(project().getBaseNode().getUuid());
			System.out.println(folder("news").getUuid());
			System.out.println(folder("2015").getUuid());
			return null;
		});

		// 2. Take folder /news offline - This should fail since folder /news/2015 is still published
		db.noTrx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, node.getUuid()), BAD_REQUEST, "node_error_children_containers_still_published");
			return null;
		});

		//3. Take sub nodes offline
		db.noTrx(() -> {
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, content("news overview").getUuid(), new TakeOfflineParameter().setRecursive(false)));
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, folder("2015").getUuid(), new TakeOfflineParameter().setRecursive(true)));
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, folder("2014").getUuid(), new TakeOfflineParameter().setRecursive(true)));
			return null;
		});

		// 4. Take folder /news offline - It should work since all child nodes have been taken offline
		db.noTrx(() -> {
			// 1. Take folder offline
			Node node = folder("news");
			call(() -> getClient().takeNodeOffline(PROJECT_NAME, node.getUuid()));
			return null;
		});

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
