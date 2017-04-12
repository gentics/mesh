package com.gentics.mesh.core.node;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractETagTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.ETag;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeEndpointETagTest extends AbstractETagTest {

	@Test
	public void testReadMultiple() {
		try (NoTx noTx = db().noTx()) {
			MeshResponse<NodeListResponse> response = client().findNodes(PROJECT_NAME).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findNodes(PROJECT_NAME), etag, true);
			expectNo304(client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true);
			expectNo304(client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(2)), etag, true);
		}
	}

	@Test
	public void testReadNodeTags() {
		try (NoTx noTx = db().noTx()) {
			Node node = content();
			String nodeUuid = node.getUuid();

			MeshResponse<TagListResponse> response = client().findTagsForNode(PROJECT_NAME, nodeUuid).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true);
			expectNo304(client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPage(2)), etag, true);
			expectNo304(client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPerPage(2)), etag, true);

			// Add another tag to the node
			call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tag("red").getUuid()));

			// We added another tag to the node thus the tags result is different
			expectNo304(client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true);
		}
	}

	@Test
	public void testReadChildren() {
		try (NoTx noTx = db().noTx()) {
			String uuid = project().getBaseNode().getUuid();
			MeshResponse<NodeListResponse> response = client().findNodeChildren(PROJECT_NAME, uuid).invoke();
			latchFor(response);
			String etag = ETag.extract(response.getResponse().getHeader(ETAG));
			assertNotNull(etag);

			expect304(client().findNodeChildren(PROJECT_NAME, uuid), etag, true);
			expectNo304(client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPage(2)), etag, true);
			expectNo304(client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPerPage(2)), etag, true);

			// Create a new node in the parent folder
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNode(new NodeReference().setUuid(uuid));
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("name", FieldUtil.createStringField("someName"));
			NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, request));

			// We added another node but it has not yet been published
			expect304(client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), etag, true);

			call(() -> client().publishNode(PROJECT_NAME, createdNode.getUuid()));

			// We published the node thus the children result is different
			expectNo304(client().findNodeChildren(PROJECT_NAME, uuid), etag, true);
		}
	}

	@Test
	public void testReadOne() {
		try (NoTx noTx = db().noTx()) {
			Node node = content();

			// Inject the reference node field
			SchemaModel schema = node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			node.getGraphFieldContainer("en").getSchemaContainerVersion().setSchema(schema);
			node.getGraphFieldContainer("en").createNode("reference", folder("2015"));

			MeshResponse<NodeResponse> response = client().findNodeByUuid(PROJECT_NAME, node.getUuid()).invoke();
			latchFor(response);
			String etag = node.getETag(mockActionContext());
			assertEquals(etag, ETag.extract(response.getResponse().getHeader(ETAG)));

			// Check whether 304 is returned for correct etag
			MeshRequest<NodeResponse> request = client().findNodeByUuid(PROJECT_NAME, node.getUuid());
			assertThat(expect304(request, etag, true)).contains(etag);

			assertNotEquals("A different etag should have been generated since we are not requesting the expanded node.", etag,
					expectNo304(client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setExpandAll(true)), etag, true));

			String newETag = expectNo304(
					client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setExpandedFieldNames("reference")), etag, true);
			assertNotEquals("We added parameters and thus a new etag should have been generated.", newETag,
					expectNo304(
							client().findNodeByUuid(PROJECT_NAME, node.getUuid(), new NodeParametersImpl().setExpandedFieldNames("reference", "bla")),
							newETag, true));

		}

	}

}
