package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.callETag;
import static com.gentics.mesh.test.ClientHelper.callETagRaw;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		String etag = callETag(() -> client().findNodes(PROJECT_NAME));
		callETag(() -> client().findNodes(PROJECT_NAME), etag, true, 304);
		callETag(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
		callETag(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(2L)), etag, true, 200);
	}

	@Test
	public void testReadWithoutETag() {
		String etag = callETagRaw(() -> client().findNodes(PROJECT_NAME, new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);

		etag = callETagRaw(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);

		String folderUuid = tx(() -> folder("2015").getUuid());
		etag = callETagRaw(() -> client().findNodeChildren(PROJECT_NAME, folderUuid, new GenericParametersImpl().setETag(false)));
		assertNull("The etag should not have been generated.", etag);
	}

	@Test
	public void testReadNodeTags() {
		String nodeUuid = contentUuid();

		String etag = callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));
		callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true, 304);
		callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
		callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPerPage(2L)), etag, true, 200);

		// Add another tag to the node
		call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tx(() -> tag("red").getUuid())));

		// We added another tag to the node thus the tags result is different
		callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true, 200);
	}

	@Test
	public void testPublishUnpublishNode() {
		String uuid = contentUuid();

		// Make sure the node is published
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		String etag = callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// Take node offline - The etag should change
		call(() -> client().takeNodeOffline(PROJECT_NAME, uuid));
		callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid), etag, true, 200);

		// Publish again - The etag is different because new versions have been created
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid), etag, true, 200);
	}

	@Test
	public void testPublishUnpublishNodeLanguage() {
		String uuid = contentUuid();

		// Add language
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("de");
		request.getFields().put("title", FieldUtil.createStringField("Title"));
		request.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		request.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		request.setVersion("1.0");
		call(() -> client().updateNode(PROJECT_NAME, uuid, request));

		// Make sure all languages are published
		call(() -> client().publishNode(PROJECT_NAME, uuid));
		String etag = callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de")));

		// Take de offline - The etag should change
		call(() -> client().takeNodeLanguageOffline(PROJECT_NAME, uuid, "de"));
		callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid), etag, true, 200);

		// Publish de again - The etag is different because new versions have been created
		call(() -> client().publishNodeLanguage(PROJECT_NAME, uuid, "de"));
		callETag(() -> client().findNodeByUuid(PROJECT_NAME, uuid), etag, true, 200);
	}

	@Test
	public void testReadChildren() {
		String uuid = tx(() -> project().getBaseNode().getUuid());
		String etag = callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid));

		callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid), etag, true, 304);
		callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
		callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPerPage(2L)), etag, true, 200);

		// Create a new node in the parent folder
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNode(new NodeReference().setUuid(uuid));
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.getFields().put("teaser", FieldUtil.createStringField("someTeaser"));
		request.getFields().put("slug", FieldUtil.createStringField("someSlug"));
		NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, request));

		// We added another node but it has not yet been published
		callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), etag, true, 304);

		call(() -> client().publishNode(PROJECT_NAME, createdNode.getUuid()));

		// We published the node thus the children result is different
		callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid), etag, true, 200);

	}

	@Test
	public void testReadOne() {
		HibNode node = content();

		try (Tx tx = tx()) {
			// Inject the reference node field
			SchemaVersionModel schema = boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().setSchema(schema);
			boot().contentDao().getGraphFieldContainer(node, "en").createNode("reference", folder("2015"));
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			String actualEtag = callETag(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
			String etag = nodeDao.getETag(node, mockActionContext());
			assertEquals(etag, actualEtag);

			// Check whether 304 is returned for correct etag
			assertThat(callETag(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()), etag, true, 304)).contains(etag);

			assertNotEquals("A different etag should have been generated since we are not requesting the expanded node.", etag, callETag(
				() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true, 200));

			String newETag = callETag(
				() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new NodeParametersImpl().setExpandedFieldNames("reference")), etag,
				true, 200);
			assertNotEquals("We added parameters and thus a new etag should have been generated.", newETag, callETag(
				() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new NodeParametersImpl().setExpandedFieldNames("reference", "bla")),
				newETag, true, 200));
		}

	}

}
