package com.gentics.mesh.search;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getRangeQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jsoup.Jsoup;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;
import com.syncleus.ferma.tx.Tx;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = FULL, startServer = true)
public class NodeSearchEndpointCTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchNumberRange() throws Exception {
		int numberValue = 1200;
		tx(() -> addNumberSpeedFieldToOneNode(numberValue));
		recreateIndices();

		// from 100 to 9000
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 100, 9000),
			new VersioningParametersImpl().draft()));
		assertEquals(1, response.getData().size());
	}

	@Test
	public void testSearchNumberRange2() throws Exception {
		int numberValue = 1200;
		tx(() -> {
			addNumberSpeedFieldToOneNode(numberValue);
			content().getLatestDraftFieldContainer(english()).createNumber("speed").setNumber(92.1535f);
		});
		recreateIndices();

		// from 9 to 1
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 900, 1500),
			new VersioningParametersImpl().draft()));
		assertEquals("We could expect to find the node with the given seed number field since the value {" + numberValue
			+ "} is between the search range.", 1, response.getData().size());
	}

	@Test
	public void testSearchNumberRange3() throws Exception {
		int numberValue = 1200;
		tx(() -> addNumberSpeedFieldToOneNode(numberValue));
		recreateIndices();

		// out of bounds
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 1000, 90),
			new VersioningParametersImpl().draft()));
		assertEquals("No node should be found since the range is invalid.", 0, response.getData().size());
	}

	@Test
	public void testSearchMicronode() throws Exception {
		tx(this::addMicronodeField);
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.vcard.fields-vcard.firstName", "Mickey"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));

		assertEquals("Check returned search results", 1, response.getData().size());
		assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());

		try (Tx tx = tx()) {
			for (NodeResponse nodeResponse : response.getData()) {
				assertNotNull("Returned node must not be null", nodeResponse);
				assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
			}
		}
	}

	@Test
	public void testSearchStringFieldNoRaw() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.teaser.raw", "Concorde_english_name"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("No results should be found since the raw field was not added to the teaser schema field", 0, response.getData().size());
	}

	@Test
	public void testSearchStringFieldRaw() throws Exception {
		addRawToSchemaField();
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.teaser.raw", "Concorde_english_name"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());
	}

	@Test
	public void testSearchStringFieldRawAfterIndexSync() throws Exception {
		addRawToSchemaField();
		recreateIndices();

		// Add the user to the admin group - this way the user is in fact an admin.
		tx(() -> user().addGroup(groups().get("admin")));

		waitForEvent(INDEX_SYNC_FINISHED, () -> {
			GenericMessageResponse message = call(() -> client().invokeIndexSync());
			assertThat(message).matches("search_admin_index_sync_invoked");
		});

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.teaser.raw", "Concorde_english_name"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());
	}

	private void addRawToSchemaField() {
		// Update the schema and enable the addRaw field
		String schemaUuid = tx(() -> content().getSchemaContainer().getUuid());
		SchemaUpdateRequest request = tx(() -> JsonUtil.readValue(content().getSchemaContainer().getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		request.getField("teaser").setElasticsearch(IndexOptionHelper.getRawFieldOption());

		tx(() -> group().addRole(roles().get("admin")));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);
		tx(() -> group().removeRole(roles().get("admin")));

	}

	@Test
	public void testSearchHtml() throws Exception {
		recreateIndices();

		String newHtml = "ABCD<strong>EF</strong>GHI";
		String newPlain = Jsoup.parse(newHtml).text();
		String nodeUuid = db().tx(() -> content("concorde").getUuid());

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());

		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newHtml));
		update.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

		waitForSearchIdleEvent();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"), new PagingParametersImpl().setPage(1)
			.setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' after update", 0, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newPlain), new PagingParametersImpl().setPage(1)
			.setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for '" + newPlain + "' after update", 1, response.getData().size());
	}

	@Test
	public void testDocumentUpdate() throws Exception {
		recreateIndices();

		String newString = "ABCDEFGHI";
		String nodeUuid = db().tx(() -> content("concorde").getUuid());

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());

		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newString));
		update.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));

		waitForSearchIdleEvent();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "supersonic"), new PagingParametersImpl().setPage(1)
			.setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for 'supersonic' after update", 0, response.getData().size());

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", newString), new PagingParametersImpl().setPage(1)
			.setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("Check hits for '" + newString + "' after update", 1, response.getData().size());
	}

	@Test
	public void testSearchContentResolveLinksAndLangFallback() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "the"),
			new PagingParametersImpl().setPage(1)
				.setPerPage(2L),
			new NodeParametersImpl().setResolveLinks(LinkType.FULL).setLanguages("de", "en"), new VersioningParametersImpl()
				.draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}
	}

	@Test
	public void testSearchContentResolveLinks() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "the"), new PagingParametersImpl()
			.setPage(1).setPerPage(2L), new NodeParametersImpl().setResolveLinks(LinkType.FULL), new VersioningParametersImpl().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

}
