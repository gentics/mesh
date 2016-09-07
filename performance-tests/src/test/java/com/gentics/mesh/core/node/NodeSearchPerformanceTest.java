package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.search.AbstractSearchVerticleTest;
import com.gentics.mesh.test.performance.StopWatchLogger;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.AbstractVerticle;

public class NodeSearchPerformanceTest extends AbstractSearchVerticleTest {

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.searchVerticle());
		list.add(meshDagger.projectSearchVerticle());
		list.add(meshDagger.nodeVerticle());
		return list;
	}

	@Test
	public void testES() throws InterruptedException, InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String lastNodeUuid = null;
		String uuid = db.noTx(() -> folder("news").getUuid());
		for (int i = 0; i < 300; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> getClient().createNode(PROJECT_NAME, request));
			lastNodeUuid = response.getUuid();
			call(() -> getClient().publishNode(PROJECT_NAME, response.getUuid()));
		}

		// Revoke all but one permission
		try (NoTx noTx = db.noTx()) {
			for (Node node : boot.nodeRoot().findAll()) {
				if (!node.getUuid().equals(lastNodeUuid)) {
					role().revokePermissions(node, READ_PERM);
				}
			}
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
		loggingStopWatch(logger, "node.search-filter-one-perm", 400, (step) -> {
			NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, search, new VersioningParameters().draft()));
			assertEquals(1, response.getMetainfo().getTotalCount());
		});

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
