package com.gentics.mesh.search;

import static org.elasticsearch.client.Requests.refreshRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SearchVerticle searchVerticle;

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Override
	public AbstractWebVerticle getVerticle() {
		return searchVerticle;
	}

	@BeforeClass
	public static void setup() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	@Test
	public void testSearchContent() throws InterruptedException {

		for (Node node : boot.nodeRoot().findAll()) {
			boot.meshRoot().getSearchQueueRoot().addElement(node);
		}

	

		assertNotNull(elasticSearchNode);

		Vertx vertx = Mesh.vertx();
		assertNotNull(vertx);

		Node node = folder("2015");

		JsonObject message = new JsonObject();
		message.put("uuid", node.getUuid());
		message.put("type", "node");
		vertx.eventBus().send("search-index-create", message);
		Thread.sleep(1000);

		elasticSearchNode.client().admin().indices().refresh(refreshRequest()).actionGet();

		QueryBuilder qb = QueryBuilders.queryStringQuery("2015");
		Future<NodeListResponse> future = getClient().searchNodes(qb.toString());
		latchFor(future);
		assertSuccess(future);
		assertEquals(1, future.result().getData().size());
	}

}
