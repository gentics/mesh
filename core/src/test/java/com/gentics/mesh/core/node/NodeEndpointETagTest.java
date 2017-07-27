package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.callETag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeEndpointETagTest extends AbstractMeshTest {

	@Test
	public void testReadMultiple() {
		try (Tx tx = tx()) {
			String etag = callETag(() -> client().findNodes(PROJECT_NAME));

			callETag(() -> client().findNodes(PROJECT_NAME), etag, true, 304);
			callETag(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPage(2)), etag, true, 200);
			callETag(() -> client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(2)), etag, true, 200);
		}
	}

	@Test
	public void testReadNodeTags() {
		Node node = content();

		try (Tx tx = tx()) {
			String nodeUuid = node.getUuid();

			String etag = callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid));
			callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true, 304);
			callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
			callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid, new PagingParametersImpl().setPerPage(2)), etag, true, 200);

			// Add another tag to the node
			call(() -> client().addTagToNode(PROJECT_NAME, nodeUuid, tag("red").getUuid()));

			// We added another tag to the node thus the tags result is different
			callETag(() -> client().findTagsForNode(PROJECT_NAME, nodeUuid), etag, true, 200);
		}
	}

	@Test
	public void testReadChildren() {
		try (Tx tx = tx()) {
			String uuid = project().getBaseNode().getUuid();
			String etag = callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid));

			callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid), etag, true, 304);
			callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPage(2)), etag, true, 200);
			callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new PagingParametersImpl().setPerPage(2)), etag, true, 200);

			// Create a new node in the parent folder
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNode(new NodeReference().setUuid(uuid));
			request.setSchema(new SchemaReference().setName("content"));
			request.getFields().put("teaser", FieldUtil.createStringField("someTeaser"));
			request.getFields().put("slug", FieldUtil.createStringField("someSlug"));
			NodeResponse createdNode = call(() -> client().createNode(PROJECT_NAME, request));

			// We added another node but it has not yet been published
			callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid, new VersioningParametersImpl().published()), etag, true, 304);

			call(() -> client().publishNode(PROJECT_NAME, createdNode.getUuid()));

			// We published the node thus the children result is different
			callETag(() -> client().findNodeChildren(PROJECT_NAME, uuid), etag, true, 200);
		}
	}

	@Test
	public void testReadOne() {
		Node node = content();

		try (Tx tx = tx()) {
			// Inject the reference node field
			SchemaModel schema = node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema();
			schema.addField(FieldUtil.createNodeFieldSchema("reference"));
			node.getGraphFieldContainer("en").getSchemaContainerVersion().setSchema(schema);
			node.getGraphFieldContainer("en").createNode("reference", folder("2015"));
			tx.success();
		}

		try (Tx tx = tx()) {
			String actualEtag = callETag(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
			String etag = node.getETag(mockActionContext());
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
