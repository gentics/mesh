package com.gentics.mesh.core.node;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.performance.StopWatch.loggingStopWatch;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.performance.StopWatchLogger;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class NodeSearchPerformanceTest extends AbstractMeshTest {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchPerformanceTest.class);

	private StopWatchLogger logger = StopWatchLogger.logger(getClass());

	@Test
	public void testES() throws Exception {
		try (Tx tx = db().tx()) {
			recreateIndices();
		}

		String lastNodeUuid = null;
		String uuid = db().tx(() -> folder("news").getUuid());
		int total = 600;
		for (int i = 0; i < total; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReferenceImpl().setName("content"));
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
		try (Tx tx = db().tx()) {
			for (Node node : project().getNodeRoot().findAll()) {
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
		json += "	                \"term\" : { \"schema.name.raw\" : \"content\" }";
		json += "	            }";
		json += "	        }";
		json += "	    }";
		json += "	}";

		String search = json;
		loggingStopWatch(logger, "node.search-filter-one-perm", 400, (step) -> {
			NodeListResponse response = call(
					() -> client().searchNodes(PROJECT_NAME, search, new VersioningParametersImpl().draft()));
			assertEquals(1, response.getMetainfo().getTotalCount());
		});

	}

	@Test
	public void testSearchAndSort() throws Exception {
		try (Tx tx = db().tx()) {
			recreateIndices();
		}

		String uuid = db().tx(() -> folder("news").getUuid());
		int total = 2000;
		for (int i = 0; i < total; i++) {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReferenceImpl().setName("content"));
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
		json += "			                \"term\" : { \"schema.name.raw\" : \"content\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		String search = json;
		loggingStopWatch(logger, "node.search-filter-schema", 200, (step) -> {
			call(() -> client().searchNodes(PROJECT_NAME, search, new VersioningParametersImpl().draft()));
		});

	}

}
