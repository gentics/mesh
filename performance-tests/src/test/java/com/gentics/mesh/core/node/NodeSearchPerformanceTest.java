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
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.search.AbstractSearchVerticleTest;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchVerticle;
import com.gentics.mesh.test.performance.StopWatchLogger;

import io.vertx.core.AbstractVerticle;

public class NodeSearchPerformanceTest extends AbstractSearchVerticleTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	protected SearchVerticle searchVerticle;

	private NodeVerticle nodeVerticle;

	protected ProjectSearchVerticle projectSearchVerticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(projectSearchVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testSearchAndSort() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String uuid = db.noTx(() -> folder("news").getUuid());
		for (int i = 0; i < 2000; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> getClient().createNode(PROJECT_NAME, request));
			call(() -> getClient().publishNode(PROJECT_NAME, response.getUuid()));
		}

		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"schema.name\" : \"content\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		String search = json;
		loggingStopWatch(logger, "node.search-filter-schema", 200, (step) -> {
			call(() -> getClient().searchNodes(PROJECT_NAME, search, new VersioningParameters().draft()));
		});

	}

}
