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
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.performance.StopWatchLogger;

@MeshTestSetting(testSize = FULL, startServer = true)
public class WebrootEndpointPerformanceTest extends AbstractMeshTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	public void addNodes() {
		String uuid = db().tx(() -> folder("news").getUuid());
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid(uuid);
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
		request.getFields().put("teaser", FieldUtil.createHtmlField("someTeaser"));
		for (int i = 0; i < 1000; i++) {
			request.getFields().put("slug", FieldUtil.createStringField("someNode_" + i));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
		}
	}

	@Test
	public void testReadSingle() {
		addNodes();
		loggingStopWatch(logger, "webroot.readSingle", 500, (step) -> {
			call(() -> client().webroot(PROJECT_NAME, "/News/someNode_999"));
		});
	}

}
