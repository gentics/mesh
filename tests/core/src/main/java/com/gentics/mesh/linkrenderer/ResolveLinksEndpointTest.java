package com.gentics.mesh.linkrenderer;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ResolveLinksEndpointTest extends AbstractMeshTest {
	private NodeResponse nodeWithReference;

	@Before
	public void setUp() throws Exception {
		BranchResponse branch = getBranch();
		branch.setSsl(true);
		branch.setHostname("gentics.com");
		client().updateBranch(PROJECT_NAME, branch.getUuid(), branch.toRequest()).blockingAwait();
		nodeWithReference = createNode("name", StringField.of("{{mesh.link('" + folderUuid() + "')}}"));
	}

	@Test
	public void testShortLink() {
		// scheme and hostname should not be appended on references to nodes of the same branch
		assertThat(getNodeWithReference(LinkType.SHORT)).hasStringField("name", "/News");
	}

	@Test
	public void testShortLinkGraphQl() {
		// scheme and hostname should not be appended on references to nodes of the same branch
		GraphQLResponse response = client().graphql(PROJECT_NAME, new GraphQLRequest()
			.setQuery("query($uuid: String) { node(uuid: $uuid) { ... on folder { fields { name(linkType: SHORT) } } } }")
			.setVariables(new JsonObject().put("uuid", nodeWithReference.getUuid()))
		).blockingGet();
		assertThat(response.getData()
			.getJsonObject("node")
			.getJsonObject("fields")
			.getString("name"))
		.isEqualTo("/News");
	}

	@Test
	public void testMediumLink() {
		// scheme and hostname should not be appended on references to nodes of the same branch
		assertThat(getNodeWithReference(LinkType.MEDIUM)).hasStringField("name", "/dummy/News");
	}

	@Test
	public void testFullLink() {
		// scheme and hostname should not be appended on references to nodes of the same branch
		assertThat(getNodeWithReference(LinkType.FULL)).hasStringField("name", "/api/v2/dummy/webroot/News");
	}

	private NodeResponse getNodeWithReference(LinkType linkType) {
		return client().findNodeByUuid(PROJECT_NAME, nodeWithReference.getUuid(), new NodeParametersImpl().setResolveLinks(linkType)).blockingGet();
	}
}
