package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;

public class NodeSearchEndpointCTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchNumberRange() throws Exception {
		int numberValue = 1200;
		try (NoTx noTx = db.noTx()) {
			addNumberSpeedFieldToOneNode(numberValue);
			fullIndex();
		}

		// from 100 to 9000
		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 100, 9000), new VersioningParameters().draft()));
		assertEquals(1, response.getData().size());
	}

	@Test
	public void testSearchNumberRange2() throws Exception {
		int numberValue = 1200;
		try (NoTx noTx = db.noTx()) {
			addNumberSpeedFieldToOneNode(numberValue);
			content().getLatestDraftFieldContainer(english()).createNumber("speed").setNumber(92.1535f);
			fullIndex();
		}

		// from 9 to 1
		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 900, 1500), new VersioningParameters().draft()));
		assertEquals("We could expect to find the node with the given seed number field since the value {" + numberValue
				+ "} is between the search range.", 1, response.getData().size());
	}

	@Test
	public void testSearchBinaryField() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Node nodeA = content("concorde");
			Node nodeB = content();
			Schema schema = nodeA.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new BinaryFieldSchemaImpl().setName("binary"));
			nodeA.getSchemaContainer().getLatestVersion().setSchema(schema);

			// image
			nodeA.getLatestDraftFieldContainer(english()).createBinary("binary").setFileName("somefile.jpg").setFileSize(200).setImageHeight(200)
					.setImageWidth(400).setMimeType("image/jpeg").setSHA512Sum("someHash").setImageDominantColor("#super");

			// file
			nodeB.getLatestDraftFieldContainer(english()).createBinary("binary").setFileName("somefile.dat").setFileSize(200)
					.setMimeType("application/test").setSHA512Sum("someHash");
			fullIndex();
		}

		// filesize
		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.filesize", 100, 300), new VersioningParameters().draft()));
		assertEquals("Exactly two nodes should be found for the given filesize range.", 2, response.getData().size());

		// width
		response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.width", 300, 500), new VersioningParameters().draft()));
		assertEquals("Exactly one node should be found for the given image width range.", 1, response.getData().size());

		// height
		response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.binary.height", 100, 300), new VersioningParameters().draft()));
		assertEquals("Exactly one node should be found for the given image height range.", 1, response.getData().size());

		// dominantColor
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.dominantColor", "#super"),
				new VersioningParameters().draft()));
		assertEquals("Exactly one node should be found for the given image dominant color.", 1, response.getData().size());

		// mimeType
		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.binary.mimeType", "image/jpeg"),
				new VersioningParameters().draft()));
		assertEquals("Exactly one node should be found for the given image mime type.", 1, response.getData().size());

	}

	@Test
	public void testSearchNumberRange3() throws Exception {
		int numberValue = 1200;
		try (NoTx noTx = db.noTx()) {
			addNumberSpeedFieldToOneNode(numberValue);
			fullIndex();
		}

		// out of bounds
		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getRangeQuery("fields.speed", 1000, 90), new VersioningParameters().draft()));
		assertEquals("No node should be found since the range is invalid.", 0, response.getData().size());
	}

	@Test
	public void testSearchMicronode() throws Exception {
		try (NoTx noTx = db.noTx()) {
			addMicronodeField();
			fullIndex();
		}

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("Mickey"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));

		assertEquals("Check returned search results", 1, response.getData().size());
		assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());

		try (NoTx noTx = db.noTx()) {
			for (NodeResponse nodeResponse : response.getData()) {
				assertNotNull("Returned node must not be null", nodeResponse);
				assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
			}
		}
	}

	@Test
	public void testSearchStringFieldRaw() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.name.raw", "Concorde_english_name"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());
	}

	@Test
	public void testSearchStringFieldRawAfterReindex() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		// Add the user to the admin group - this way the user is in fact an admin.
		try (NoTx noTrx = db.noTx()) {
			user().addGroup(groups().get("admin"));
			searchProvider.refreshIndex();
		}

		GenericMessageResponse message = call(() -> getClient().invokeReindex());
		expectResponseMessage(message, "search_admin_reindex_invoked");

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("fields.name.raw", "Concorde_english_name"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());
	}

	@Test
	public void testDocumentUpdate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String newString = "ABCDEFGHI";
		String nodeUuid = db.noTx(() -> content("concorde").getUuid());

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));
		assertEquals("Check hits for 'supersonic' before update", 1, response.getData().size());

		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage("en");
		update.getFields().put("content", FieldUtil.createHtmlField(newString));
		update.setVersion(new VersionReference().setNumber("1.0"));
		call(() -> getClient().updateNode(PROJECT_NAME, nodeUuid, update));

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("supersonic"), new PagingParameters().setPage(1).setPerPage(2),
				new VersioningParameters().draft()));
		assertEquals("Check hits for 'supersonic' after update", 0, response.getData().size());

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery(newString), new PagingParameters().setPage(1).setPerPage(2),
				new VersioningParameters().draft()));
		assertEquals("Check hits for '" + newString + "' after update", 1, response.getData().size());
	}

	@Test
	public void testSearchContentResolveLinksAndLangFallback() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("the"), new PagingParameters().setPage(1).setPerPage(2),
						new NodeParameters().setResolveLinks(LinkType.FULL).setLanguages("de", "en"), new VersioningParameters().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}
	}

	@Test
	public void testSearchContentResolveLinks() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("the"), new PagingParameters().setPage(1).setPerPage(2),
						new NodeParameters().setResolveLinks(LinkType.FULL), new VersioningParameters().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

}
