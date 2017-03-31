package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.graphdb.NoTx;
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
		testQueries.add("node-elasticsearch-query");
		return testQueries;
	}

	private final String queryName;

	public GraphQLSearchEndpointTest(String queryName) {
		this.queryName = queryName;
	}

	@Test
	public void testNodeQuery() throws Exception {
		try (NoTx noTx = db().noTx()) {
			recreateIndices();
		}
		JsonObject response = call(() -> client().graphqlQuery(PROJECT_NAME, getQuery(queryName)));
		System.out.println(response.encodePrettily());
		assertThat(response).compliesToAssertions(queryName);
	}

}
