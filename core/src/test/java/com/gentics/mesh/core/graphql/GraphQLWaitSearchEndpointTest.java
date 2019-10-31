package com.gentics.mesh.core.graphql;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.impl.GraphQLParametersImpl;
import com.gentics.mesh.search.AbstractMultiESTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class GraphQLWaitSearchEndpointTest extends AbstractMultiESTest {
	private final String queryName = "wait-query";
	private final String slugContent = "waittest";

	public GraphQLWaitSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Before
	public void setUp() throws Exception {
		// TODO: Add proper delay
		createNode("slug", new StringFieldImpl().setString(slugContent));
	}

	@Test
	public void testWithWait() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new GraphQLParametersImpl().setWait(true)));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		MeshAssertions.assertThat(json).has("data.nodes.elements[0]fields.slug", slugContent, "The search should find the element after wait");
	}

	@Test
	public void testWithoutWait() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName), new GraphQLParametersImpl().setWait(false)));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		MeshAssertions.assertThat(json).has("data.nodes.elements[0]fields.slug", slugContent, "The search should not find the element without wait");
	}
}
