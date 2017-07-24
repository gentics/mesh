package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLSearchEndpointTest extends AbstractMeshTest {

	@Parameters(name = "query={0}")
	public static List<String> paramData() {
		List<String> testQueries = new ArrayList<>();
		testQueries.add("user-elasticsearch-query");
		testQueries.add("group-elasticsearch-query");
		testQueries.add("role-elasticsearch-query");
		testQueries.add("node-elasticsearch-query");
		testQueries.add("tag-elasticsearch-query");
		testQueries.add("tagFamily-elasticsearch-query");
		return testQueries;
	}

	private final String queryName;

	public GraphQLSearchEndpointTest(String queryName) {
		this.queryName = queryName;
	}

	@Test
	public void testNodeQuery() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(JsonUtil.toJson(response));
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

}
