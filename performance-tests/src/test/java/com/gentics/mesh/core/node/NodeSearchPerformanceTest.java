package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.search.AbstractSearchEndpointTest;
import com.gentics.mesh.test.performance.StopWatchLogger;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeSearchPerformanceTest extends AbstractSearchEndpointTest {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchPerformanceTest.class);

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Test
	public void testES() throws Exception {
		try (NoTx noTx = db.noTx()) {
			recreateIndices();
		}

		String lastNodeUuid = null;
		String uuid = db.noTx(() -> folder("news").getUuid());
		int total = 600;
		for (int i = 0; i < total; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			lastNodeUuid = response.getUuid();
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
			if (i % 100 == 0) {
				log.info("Created " + i + " of " + total + " nodes.");
			}
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
		json += "	\"sort\" : {";
		json += "	      \"created\" : {\"order\" : \"asc\"}";
		json += "	    },";
		json += "	    \"query\":{";
		json += "	        \"bool\" : {";
		json += "	            \"must\" : {";
		json += "	                \"term\" : { \"schema.name\" : \"content\" }";
		json += "	            }";
		json += "	        }";
		json += "	    }";
		json += "	}";

		String search = json;
		loggingStopWatch(logger, "node.search-filter-one-perm", 400, (step) -> {
			NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, search, new VersioningParameters().draft()));
			assertEquals(1, response.getMetainfo().getTotalCount());
		});

	}

	@Test
	public void testSearchAndSort() throws Exception {
		try (NoTx noTx = db.noTx()) {
			recreateIndices();
		}

		String uuid = db.noTx(() -> folder("news").getUuid());
		int total = 2000;
		for (int i = 0; i < total; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someNode_" + i));
			request.getFields().put("content", FieldUtil.createHtmlField("someContent"));
			NodeResponse response = call(() -> client().createNode(PROJECT_NAME, request));
			call(() -> client().publishNode(PROJECT_NAME, response.getUuid()));
			if (i % 100 == 0) {
				log.info("Created " + i + " of " + total + " nodes.");
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
		loggingStopWatch(logger, "node.search-filter-schema", 200, (step) -> {
			call(() -> client().searchNodes(PROJECT_NAME, search, new VersioningParameters().draft()));
		});

	}

}
