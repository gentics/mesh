package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLUserScrollSearchEndpointTest extends AbstractMeshTest {

	@Parameters(name = "query={0}")
	public static List<String> paramData() {
		List<String> testQueries = new ArrayList<>();
		testQueries.add("user-elasticsearch-scroll-query");
		return testQueries;
	}

	private final String queryName;

	public GraphQLUserScrollSearchEndpointTest(String queryName) {
		this.queryName = queryName;
	}

	@Before
	public void createUsers() {
		String username = "testuser";
		try (Tx tx = tx()) {
			for (int i = 0; i < 100; i++) {
				createUser(username + i);
			}
		}
	}

	@Test
	public void testNodeQuery() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

}
