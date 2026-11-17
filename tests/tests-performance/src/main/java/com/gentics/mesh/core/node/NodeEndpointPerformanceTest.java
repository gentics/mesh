package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeEndpointPerformanceTest extends AbstractMeshTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	public void addNodes() {
		String uuid = db().tx(() -> folder("news").getUuid());
		for (int i = 0; i < 500; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			request.getFields().put("slug", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
		}
	}

	@Test
	public void testReadNav() {
		addNodes();
		String baseUuid = db().tx(() -> project().getBaseNode().getUuid());
		loggingStopWatch(logger, "node.read-nav-expanded-full-4", 200, (step) -> {
			call(() -> client().loadNavigation(PROJECT_NAME, baseUuid, new NodeParametersImpl().setExpandAll(true).setResolveLinks(LinkType.FULL),
					new NavigationParametersImpl().setMaxDepth(4)));
		});
	}

	@Test
	public void testReadPage() {
		addNodes();
		loggingStopWatch(logger, "node.read-page-100", 200, (step) -> {
			call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(100L)));
		});

		loggingStopWatch(logger, "node.read-page-25", 200, (step) -> {
			call(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(25L)));
		});
	}

	@Test
	public void testReadSingle() {
		String uuid = db().tx(() -> folder("news").getUuid());
		loggingStopWatch(logger, "node.read-by-uuid", 7000, (step) -> {
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		});

		loggingStopWatch(logger, "node.read-by-uuid-full", 800, (step) -> {
			call(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setExpandAll(true).setResolveLinks(LinkType.FULL)));
		});
	}

	@Test
	public void testCreate() {
		String uuid = db().tx(() -> folder("news").getUuid());
		loggingStopWatch(logger, "node.create", 200, (step) -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("content"));
			request.setLanguage("en");
			request.getFields().put("title", FieldUtil.createStringField("some title"));
			request.getFields().put("name", FieldUtil.createStringField("some name"));
			request.getFields().put("fileName", FieldUtil.createStringField("new-page_" + step + ".html"));
			request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
			request.setParentNodeUuid(uuid);
			call(() -> client().createNode(PROJECT_NAME, request));
		});
	}
}
