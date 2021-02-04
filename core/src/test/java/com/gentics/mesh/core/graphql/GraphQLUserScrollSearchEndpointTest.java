package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
@RunWith(Parameterized.class)
public class GraphQLUserScrollSearchEndpointTest extends AbstractGraphQLSearchEndpointTest {

	@Parameters(name = "query={0}")
	public static List<String> paramData() {
		List<String> testQueries = new ArrayList<>();
		testQueries.add("user-elasticsearch-scroll-query");
		return testQueries;
	}


	public GraphQLUserScrollSearchEndpointTest(String queryName) {
		super(queryName);
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

}
