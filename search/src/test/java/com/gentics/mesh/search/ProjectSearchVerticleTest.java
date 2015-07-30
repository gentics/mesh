package com.gentics.mesh.search;

import static org.junit.Assert.assertNotNull;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectSearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectSearchVerticle searchVerticle;

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Override
	public AbstractWebVerticle getVerticle() {
		return searchVerticle;
	}

	@Test
	public void testSearchContent() throws InterruptedException {
		assertNotNull(elasticSearchNode);

		Vertx vertx = Mesh.vertx();
		assertNotNull(vertx);

		Node node = folder("2015");

		JsonObject message = new JsonObject();
		message.put("uuid", node.getUuid());
		message.put("type", "node");
		vertx.eventBus().send("search-index-create", message);
		Thread.sleep(10000);
	}

}
