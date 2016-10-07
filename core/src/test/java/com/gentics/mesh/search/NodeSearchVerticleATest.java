package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.performance.TestUtils;

public class NodeSearchVerticleATest extends AbstractNodeSearchVerticleTest {

	@Test
	public void testSearchAndSort() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
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
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, search, new VersioningParameters().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

		long lastCreated = 0;
		for (NodeResponse nodeResponse : response.getData()) {
			Date date = Date.from(Instant.parse(nodeResponse.getCreated()));
			if (lastCreated > date.getTime()) {
				fail("Found entry that was not sorted by create timestamp. Last entry: {" + lastCreated + "} current entry: {"
						+ nodeResponse.getCreated() + "}");
			} else {
				lastCreated = date.getTime();
			}
			assertEquals("content", nodeResponse.getSchema().getName());
		}
	}

	@Test
	public void testSearchContent() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("the"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

	/**
	 * Test searching for a node which is only persisted in the search index but no longer in the graph.
	 * 
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchMissingVertex() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();

			Node node = content("honda nr");
			node.getImpl().remove();
			NodeListResponse response = call(() -> getClient().searchNodes(getSimpleQuery("the"), new PagingParameters().setPage(1).setPerPage(2)));
			assertEquals(0, response.getData().size());
			assertEquals(0, response.getMetainfo().getTotalCount());
		}
	}

	@Test
	public void testReindexNodeIndex() throws Exception {

		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db.noTx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// // Add the user to the admin group - this way the user is in fact an admin.
		try (NoTx noTrx = db.noTx()) {
			user().addGroup(groups().get("admin"));
		}

		// Now clear all data
		searchProvider.clear();

		GenericMessageResponse message = call(() -> getClient().invokeReindex());
		expectResponseMessage(message, "search_admin_reindex_invoked");

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

	}

	@Test
	public void testSearchPublishedNodes() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		// "urschnell" not found in published nodes
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		String uuid = db.noTx(() -> content("concorde").getUuid());

		// publish the Concorde
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		// "supersonic" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> getClient().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		// "supersonic" still found, "urschnell" not found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();

		// publish content "urschnell"
		call(() -> getClient().publishNode(PROJECT_NAME, db.noTx(() -> content("concorde").getUuid())));

		// "supersonic" no longer found, but "urschnell" found in published nodes
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent)));
		assertThat(response.getData()).as("Published search result").isEmpty();
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent)));
		assertThat(response.getData()).as("Published search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchDraftNodes() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String oldContent = "supersonic";
		String newContent = "urschnell";
		String uuid = db.noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		// change draft version of content
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newContent));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> getClient().updateNode(PROJECT_NAME, concorde.getUuid(), update));

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(oldContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newContent), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

	@Test
	public void testSearchPublishedInRelease() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String uuid = db.noTx(() -> content("concorde").getUuid());
		NodeResponse concorde = call(() -> getClient().findNodeByUuid(PROJECT_NAME, uuid, new VersioningParameters().draft()));
		call(() -> getClient().publishNode(PROJECT_NAME, uuid));

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());
		ReleaseCreateRequest createRelease = new ReleaseCreateRequest();
		createRelease.setName("newrelease");
		call(() -> getClient().createRelease(PROJECT_NAME, createRelease));
		failingLatch(latch);

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic")));
		assertThat(response.getData()).as("Search result").isEmpty();

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"),
				new VersioningParameters().setRelease(db.noTx(() -> project().getInitialRelease().getName()))));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

}
