package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.performance.TestUtils;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class NodeSearchEndpointBTest extends AbstractNodeSearchEndpointTest {

	/**
	 * Search in only english language versions of nodes
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchEnglish() throws Exception {
		searchWithLanguages("en");
	}

	/**
	 * Search in only German language versions of nodes
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchGerman() throws Exception {
		searchWithLanguages("de");
	}

	/**
	 * Search for string which can be found in two language variants of a single node. We would expect two nodes in the result which have different language
	 * properties.
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchMultipleLanguages() throws Exception {
		searchWithLanguages("de", "en");
	}

	@Test
	public void testSearchMicronodeResolveLinks() throws Exception {
		try (NoTx noTx = db().noTx()) {
			addMicronodeField();
			recreateIndices();
		}

		NodeListResponse response = call(
				() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("Mickey"), new PagingParametersImpl().setPage(1).setPerPage(2),
						new NodeParameters().setResolveLinks(LinkType.FULL), new VersioningParameters().draft()));

		assertEquals("Check returned search results", 1, response.getData().size());
		assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());

		try (NoTx noTx = db().noTx()) {
			for (NodeResponse nodeResponse : response.getData()) {
				assertNotNull("Returned node must not be null", nodeResponse);
				assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
			}
		}
	}

	@Test
	public void testSearchListOfMicronodes() throws Exception {
		try (NoTx noTx = db().noTx()) {
			addMicronodeListField();
			recreateIndices();
		}

		for (String firstName : Arrays.asList("Mickey", "Donald")) {
			for (String lastName : Arrays.asList("Mouse", "Duck")) {
				// valid names always begin with the same character
				boolean expectResult = firstName.substring(0, 1).equals(lastName.substring(0, 1));

				NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getNestedVCardListSearch(firstName, lastName),
						new PagingParametersImpl().setPage(1).setPerPage(2), new VersioningParameters().draft()));

				if (expectResult) {
					assertEquals("Check returned search results", 1, response.getData().size());
					assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
					for (NodeResponse nodeResponse : response.getData()) {
						assertNotNull("Returned node must not be null", nodeResponse);
						assertEquals("Check result uuid", db().noTx(() -> content("concorde").getUuid()), nodeResponse.getUuid());
					}
				} else {
					assertEquals("Check returned search results", 0, response.getData().size());
					assertEquals("Check total search results", 0, response.getMetainfo().getTotalCount());
				}
			}
		}
	}

	@Test
	public void testSearchListOfNodes() throws Exception {
		try (NoTx noTx = db().noTx()) {
			addNodeListField();
			recreateIndices();
		}

		// TODO do actual search (currently, we just test that indexing works with the mappings)
	}

	@Test
	public void testSearchDraftInRelease() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}

		NodeResponse concorde = call(
				() -> client().findNodeByUuid(PROJECT_NAME, db().noTx(() -> content("concorde").getUuid()), new VersioningParameters().draft()));

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		ReleaseCreateRequest createRelease = new ReleaseCreateRequest();
		createRelease.setName("newrelease");
		call(() -> client().createRelease(PROJECT_NAME, createRelease));
		failingLatch(latch);

		NodeListResponse response = call(
				() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"), new VersioningParameters().draft()));
		assertThat(response.getData()).as("Search result").isEmpty();

		String releaseName = db().noTx(() -> project().getInitialRelease().getName());
		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"),
				new VersioningParameters().setRelease(releaseName).draft()));
		assertThat(response.getData()).as("Search result").usingElementComparatorOnFields("uuid").containsOnly(concorde);
	}

}
