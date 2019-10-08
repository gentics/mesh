package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLSearchEndpointTest extends AbstractGraphQLSearchEndpointTest {

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

	public GraphQLSearchEndpointTest(String queryName) {
		super(queryName);
	}

}
