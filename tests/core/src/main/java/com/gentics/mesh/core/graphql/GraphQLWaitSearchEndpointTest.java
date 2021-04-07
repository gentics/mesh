package com.gentics.mesh.core.graphql;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.search.AbstractMultiESTest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class GraphQLWaitSearchEndpointTest extends AbstractMultiESTest {
	private final String QUERY_NAME = "wait-query";
	private final String FIELD_NAME = "slug";
	private final String FIELD_VALUE = "waittest";

	public GraphQLWaitSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Before
	public void setUp() {
		createNode(FIELD_NAME, new StringFieldImpl().setString(FIELD_VALUE));
	}

	@Test
	public void queryWithWait() {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME),
				new SearchParametersImpl().setWait(true)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
				.getJsonObject("data", new JsonObject())
				.getJsonObject("nodes", new JsonObject())
				.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);
	}

	@Test
	public void queryWithoutWait() {
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME),
				new SearchParametersImpl().setWait(false)));
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
				.getJsonObject("data", new JsonObject())
				.getJsonObject("nodes", new JsonObject())
				.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(0);
	}
}
