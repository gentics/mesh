package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

import io.vertx.core.AbstractVerticle;

public class NodeVerticlePerformanceTest extends AbstractIsolatedRestVerticleTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.nodeVerticle());
		return list;
	}

	public void addNodes() {
		String uuid = db.noTx(() -> folder("news").getUuid());
		for (int i = 0; i < 500; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> getClient().createNode(PROJECT_NAME, request));
			call(() -> getClient().publishNode(PROJECT_NAME, response.getUuid()));
		}
	}

	@Test
	public void testPerformance() {
		addNodes();
		String uuid = db.noTx(() -> folder("news").getUuid());
		String baseUuid = db.noTx(() -> project().getBaseNode().getUuid());

		loggingStopWatch(logger, "node.read-page-100", 200, (step) -> {
			call(() -> getClient().findNodes(PROJECT_NAME, new PagingParameters().setPerPage(100)));
		});

		loggingStopWatch(logger, "node.read-page-25", 200, (step) -> {
			call(() -> getClient().findNodes(PROJECT_NAME, new PagingParameters().setPerPage(25)));
		});

		loggingStopWatch(logger, "node.read-nav-expanded-full-4", 200, (step) -> {
			call(() -> getClient().loadNavigation(PROJECT_NAME, baseUuid, new NodeParameters().setExpandAll(true).setResolveLinks(LinkType.FULL),
					new NavigationParameters().setMaxDepth(4)));
		});

		loggingStopWatch(logger, "node.read-by-uuid", 400, (step) -> {
			call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid));
		});

		loggingStopWatch(logger, "node.create", 200, (step) -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReference().setName("content"));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("filename", FieldUtil.createStringField("new-page_" + step + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);
			call(() -> getClient().createNode(PROJECT_NAME, request));
		});
	}

}
