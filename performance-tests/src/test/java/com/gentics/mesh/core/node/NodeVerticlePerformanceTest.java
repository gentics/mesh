package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.node.PerformanceTestUtils.mark;
import static com.gentics.mesh.core.node.PerformanceTestUtils.measureAndAssert;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.StopWatch;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeVerticlePerformanceTest extends AbstractIsolatedRestVerticleTest {

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
	public void testReadPerformance() {
		StopWatch.stopWatch("node.read", 2000, (step) -> {
			call(() -> getClient().findNodes(PROJECT_NAME, new PagingParameters().setPerPage(100)));
		});
	}

	@Test
	public void testCreatePerformance() throws Exception {
		String uuid = db.noTrx(() -> folder("news").getUuid());
		StopWatch.stopWatch("node.create", 200, (step) -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content"));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page_" + step + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);

			Future<NodeResponse> future = getClient().createNode(PROJECT_NAME, request);
			latchFor(future);
			assertSuccess(future);
		});
		//measureAndAssert(nRuns, 0.059f, 4.0f);
	}

}
