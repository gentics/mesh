package com.gentics.mesh.core.node;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.util.ETag;

import io.vertx.core.AbstractVerticle;

public class NodeVerticleETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db.noTx()) {
			MeshResponse<NodeListResponse> response = getClient().findNodes(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findNodes(PROJECT_NAME), etag, true);
			expectNo304(getClient().findNodes(PROJECT_NAME, new PagingParameters().setPage(2)), etag, true);
			expectNo304(getClient().findNodes(PROJECT_NAME, new PagingParameters().setPerPage(2)), etag, true);
		}
	}

	@Test
	public void testReadNodeTags() {
		try (NoTx noTx = db.noTx()) {
			Node node = content();
			String nodeUuid = node.getUuid();

			MeshResponse<TagListResponse> response = getClient().findTagsForNode(PROJECT_NAME, nodeUuid).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true);
			expectNo304(getClient().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParameters().setPage(2)), etag, true);
			expectNo304(getClient().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParameters().setPerPage(2)), etag, true);

			// Add another tag to the node
			call(() -> getClient().addTagToNode(PROJECT_NAME, nodeUuid, tag("red").getUuid()));

			// We added another tag to the node thus the tags result is different
			expectNo304(getClient().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true);
		}
	}

	@Test
	public void testReadChildren() {
		try (NoTx noTx = db.noTx()) {
			String uuid = project().getBaseNode().getUuid();
			MeshResponse<NodeListResponse> response = getClient().findNodeChildren(PROJECT_NAME, uuid).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(getClient().findNodeChildren(PROJECT_NAME, uuid), etag, true);
			expectNo304(getClient().findNodeChildren(PROJECT_NAME, uuid, new PagingParameters().setPage(2)), etag, true);
			expectNo304(getClient().findNodeChildren(PROJECT_NAME, uuid, new PagingParameters().setPerPage(2)), etag, true);

			// Create a new node in the parent folder
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(uuid);
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someName"));
			NodeResponse createdNode = call(() -> getClient().createNode(PROJECT_NAME, request));

			// We added another node but it has not yet been published
			expect304(getClient().findNodeChildren(PROJECT_NAME, uuid), etag, true);

			call(() -> getClient().publishNode(PROJECT_NAME, createdNode.getUuid()));

			// We published the node thus the children result is different
			expectNo304(getClient().findNodeChildren(PROJECT_NAME, uuid), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db.noTx()) {
			Node node = content();

			// Inject the reference node field
			Schema schema = node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			node.getGraphFieldContainer("en").getSchemaContainerVersion().setSchema(schema);
			node.getGraphFieldContainer("en").createNode("reference", folder("2015"));

			MeshResponse<NodeResponse> response = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid()).invoke();
			latchFor(response);
			String etag = node.getETag(getMockedInternalActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<NodeResponse> request = getClient().findNodeByUuid(PROJECT_NAME, node.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			assertNotEquals("A different etag should have been generated since we are not requesting the expanded node.", etag,
					expectNo304(getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParameters().setExpandAll(true)), etag, true));

			String newETag = expectNo304(
					getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParameters().setExpandedFieldNames("reference")), etag, true);
			assertNotEquals("We added parameters and thus a new etag should have been generated.", newETag,
					expectNo304(
							getClient().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParameters().setExpandedFieldNames("reference", "bla")),
							newETag, true));

		}

	}

}
